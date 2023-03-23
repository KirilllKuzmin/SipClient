package SipCore;

import javax.sip.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import static java.awt.EventQueue.invokeLater;
import static javax.swing.GroupLayout.Alignment.*;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.UIManager.getInstalledLookAndFeels;
import static javax.swing.UIManager.setLookAndFeel;


public class SipClient extends JFrame implements SipListener {

    private JTextArea textArea = new JTextArea();
    private JTextField textField = new JTextField();

    //Объекты для подключения
    SipFactory sipFactory;
    SipStack sipStack;
    SipProvider sipProvider;
    MessageFactory messageFactory;
    HeaderFactory headerFactory;
    AddressFactory addressFactory;
    ListeningPoint listeningPoint;
    Properties properties;

    String ip;
    int port = 8080;
    String protocol = "udp";
    int tag = (new Random()).nextInt();
    Address contactAddress;
    ContactHeader contactHeader;

    public SipClient() {
        createForm();
    }

    //форма
    private void createForm() {

        JScrollPane scrollPane = new JScrollPane();
        JButton buttonConnect = new JButton();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("SIP Client");
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent event) {
                clientInitialization(event);
            }
        });

        textArea.setEditable(false);
        textArea.setColumns(20);
        textArea.setRows(5);
        scrollPane.setViewportView(textArea);

        buttonConnect.setText("Connect");
        buttonConnect.addActionListener(this::registerOnServer);

        textField.setText("sip:localhost:" + this.port);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(5)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(scrollPane)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(buttonConnect, PREFERRED_SIZE, 100, PREFERRED_SIZE)
                                                .addGap(400)
                                        )
                                        .addComponent(textField))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(TRAILING, layout.createSequentialGroup()
                                .addGap(5)
                                .addComponent(textField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                                .addComponent(scrollPane, DEFAULT_SIZE, 600, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(buttonConnect))
                                        .addGap(5))
        );

        pack();
    }

    private void clientInitialization(java.awt.event.WindowEvent event) {
        try {
            //получаем ip и создаем экземпляра для регистрации на сервере
            this.ip = InetAddress.getLocalHost().getHostAddress();
            this.sipFactory = SipFactory.getInstance();
            this.sipFactory.setPathName("gov.nist");
            this.properties = new Properties();
            this.properties.setProperty("javax.sip.STACK_NAME", "stack");
            this.sipStack = this.sipFactory.createSipStack(this.properties);
            this.messageFactory = this.sipFactory.createMessageFactory();
            this.headerFactory = this.sipFactory.createHeaderFactory();
            this.addressFactory = this.sipFactory.createAddressFactory();
            this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.port, this.protocol);
            this.sipProvider = this.sipStack.createSipProvider(this.listeningPoint);
            this.sipProvider.addSipListener(this);
            this.contactAddress = this.addressFactory.createAddress("sip:" + this.ip + ":" + this.port);
            this.contactHeader = this.headerFactory.createContactHeader(contactAddress);

            this.textArea.append("Local address: " + this.ip + ":" + this.port + "\n");
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    private void registerOnServer(ActionEvent event) {
        try {
            Address addressTo = this.addressFactory.createAddress(this.textField.getText());
            javax.sip.address.URI requestURI = addressTo.getURI();

            //Headers
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip, this.port, "udp", null);
            viaHeaders.add(viaHeader);
            MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);
            CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
            CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L,"REGISTER");
            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress, String.valueOf(this.tag));
            ToHeader toHeader = this.headerFactory.createToHeader(addressTo, null);

            Request request = this.messageFactory.createRequest(
                    requestURI,
                    "REGISTER",
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader);

            request.addHeader(contactHeader);

            //Создание и отправка транзакции.
            ClientTransaction transaction = this.sipProvider.getNewClientTransaction(request);
            transaction.sendRequest();

            this.textArea.append("Request sent:\n" + request.toString() + "\n\n");
        }
        catch(Exception e) {
            this.textArea.append("Request sent failed: " + e.getMessage() + "\n");
        }
    }


    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SipClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        invokeLater(() -> new SipClient().setVisible(true));
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {

        Response response = responseEvent.getResponse();
        this.textArea.append("\nReceived response: " + response.toString());

    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        throw new UnsupportedOperationException("Not supported");
    }
}
