/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.mailer;

import org.apache.felix.ipojo.annotations.*;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * A implementation of the Mail Sender Service using SMTP.
 * This implementation can delegate to a <em>mock</em> version, just printing the message on the console but not
 * actually sending the mail.
 * Unlike the original chameleon service, this service does not use the Event Admin.
 */
@Component
@Provides
@Instantiate
public class Smtp implements MailSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Smtp.class.getName());

    public static final String MOCK_SERVER_NAME = "mock";
    public static final String DEFAULT_FROM = "mock-mailer@wisdom-framework.org";

    private static final String CONFHOST = "mail.smtp.host";
    private static final String CONFPORT = "mail.smtp.port";
    private static final String CONFAUTH = "mail.smtp.auth";
    private static final String SEPARATOR = "\t----";


    @Requires
    ApplicationConfiguration configuration;

    /**
     * Configuration properties.
     */
    private Properties properties;

    /**
     * Enables / Disabled debugging.
     */
    private boolean debug;

    /**
     * The mail address of the sender.
     */
    @ServiceProperty(name = MailSenderService.FROM_PROPERTY)
    protected String from;

    /**
     * The name of the sender.
     */
    protected String from_name;

    /**
     * The port.
     */
    protected int port;

    /**
     * The host.
     */
    protected String host;

    /**
     * The username.
     */
    protected String username;

    /**
     * The password.
     */
    protected String password;
    /**
     * The authenticator used for SSL.
     */
    private Authenticator sslAuthentication;

    /**
     * True we should use the mock server.
     */
    protected boolean useMock;
    protected Boolean useSmtps;
    protected Connection connection;

    /**
     * Configures the sender.
     */
    @Validate
    protected void configure() {
        host = configuration.getWithDefault(CONFHOST, MOCK_SERVER_NAME);
        from = configuration.getWithDefault("mail.smtp.from", DEFAULT_FROM);
        from_name = configuration.getWithDefault("mail.smtp.from.name", null);
        useMock = MOCK_SERVER_NAME.equals(host);

        properties = new Properties();
        useSmtps = configuration.getBooleanWithDefault("mail.smtps", false);
        if (!useSmtps) {
            port = configuration.getIntegerWithDefault(CONFPORT, 25);
        } else {
            port = configuration.getIntegerWithDefault(CONFPORT, 465);
        }

        properties.put(CONFHOST, host);
        properties.put(CONFPORT, port);
        properties.put("mail.smtps.quitwait", configuration.getBooleanWithDefault("mail.smtp.quitwait", false));

        connection = Connection.valueOf(configuration.getWithDefault("mail.smtp.connection",
                Connection.NO_AUTH.toString()));

        username = configuration.get("mail.smtp.username");
        password = configuration.get("mail.smtp.password");

        debug = configuration.getBooleanWithDefault("mail.smtp.debug", false);

        manageConnectionType();

        LOGGER.info("Configuring Wisdom Mailer with:");
        @SuppressWarnings("unchecked") final Enumeration<String> enumeration =
                (Enumeration<String>) properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            LOGGER.info("\t" + name + ": " + properties.get(name));
        }
        if (username != null) {
            LOGGER.info("\tusername: " + username);
        }
        if (password != null) {
            LOGGER.info("\tpassword set but not displayed");
        }
        LOGGER.info("\tfrom: " + from);
    }

    private void manageConnectionType() {
        switch (connection) {
            case SSL:
                properties.put(CONFAUTH, Boolean.toString(true));
                properties.put("mail.smtp.socketFactory.port", Integer.toString(port));
                properties.put("mail.smtp.socketFactory.class", javax.net.ssl.SSLSocketFactory.class.getName());
                sslAuthentication = new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                };
                break;
            case TLS:
                properties.put(CONFAUTH, Boolean.toString(true));
                properties.put("mail.smtp.starttls.enable", Boolean.toString(true));
                break;
            case NO_AUTH:
            default:
                properties.put(CONFAUTH, Boolean.toString(false));
                break;
        }
    }

    @Updated
    public void reconfigure() {
        LOGGER.info("Reconfiguring the Wisdom Mailer");
        useMock = false;
        configure();
    }

    /**
     * Sends a mail.
     *
     * @param to      to
     * @param cc      cc
     * @param subject subject
     * @param body    body
     * @throws Exception if the mail cannot be sent.
     * @see org.ow2.chameleon.mail.MailSenderService#send(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    public void send(String to, String cc, String subject, String body)
            throws Exception {
        send(to, cc, subject, body, null);
    }

    /**
     * Sends a mail.
     *
     * @param to          to
     * @param cc          cc
     * @param subject     subject
     * @param body        body
     * @param attachments list of attachments
     * @throws Exception if the mail cannot be sent
     * @see org.ow2.chameleon.mail.MailSenderService#send(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.util.List)
     */
    public void send(String to, String cc, String subject, String body,
                     List<File> attachments) throws Exception {
        if (attachments != null && !attachments.isEmpty()) {
            send(new Mail()
                    .to(to)
                    .cc(cc)
                    .subject(subject)
                    .body(body)
                    .attach(attachments));
        } else {
            send(new Mail()
                    .to(to)
                    .cc(cc)
                    .subject(subject)
                    .body(body));
        }
    }

    /**
     * Sends the given mail object.
     * The mail to be sent must have a valid `to` clause, meaning not {@literal null} or empty,
     * and none of the addresses must be {@literal null}.
     *
     * @param mail the mail
     * @throws MessagingException if the mail cannot be sent.
     */
    public void send(Mail mail) throws MessagingException, UnsupportedEncodingException {
        if (mail.to() == null || mail.to().isEmpty()) {
            throw new IllegalArgumentException("The given 'to' is null or empty");
        }

        if (mail.from() == null) {
            mail.from(from);
        }

        Transport transport = null;
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Smtp.class.getClassLoader());
            Session session = Session.getDefaultInstance(properties, sslAuthentication);

            session.setDebug(debug);
            // create a message
            MimeMessage msg = new MimeMessage(session);
            if(from_name!=null){
                msg.setFrom(new InternetAddress(from, from_name));
            }else {
                msg.setFrom(new InternetAddress(from));    
            }
            

            // Manage to and cc
            msg.setRecipients(Message.RecipientType.TO, convert(mail.to()));
            msg.setRecipients(Message.RecipientType.CC, convert(mail.cc()));

            msg.setSubject(mail.subject());

            Date sent = new Date();
            msg.setSentDate(sent);
            mail.sent(sent);

            addBodyToMessage(mail, msg);

            if (useMock) {
                sendMessageWithMockServer(mail, msg);
                return;
            }

            if (useSmtps) {
                transport = session.getTransport("smtps");
            } else {
                transport = session.getTransport("smtp");
            }

            if (connection == Connection.TLS) {
                transport.connect(host,
                        port, username, password);
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, msg.getAllRecipients());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            if (transport != null) {
                transport.close();
            }
        }
    }

    private void addBodyToMessage(Mail mail, MimeMessage msg) throws MessagingException {
        // create the Multipart and its parts to it
        Multipart mp = new MimeMultipart();

        // create and fill the first message part
        MimeBodyPart body = new MimeBodyPart();
        body.setText(mail.body(), mail.charset(), mail.subType());
        mp.addBodyPart(body);

        List<File> attachments = mail.attachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (File file : attachments) {
                MimeBodyPart part = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                part.setDataHandler(new DataHandler(source));
                part.setFileName(file.getName());
                mp.addBodyPart(part);
            }
        }

        // add the Multipart to the message
        msg.setContent(mp);
    }

    /**
     * Prints the mail info with the logger.
     *
     * @param mail the mail
     * @param msg  the mime message
     * @throws MessagingException if the message if malformed.
     */
    private void sendMessageWithMockServer(Mail mail, MimeMessage msg) throws MessagingException {
        Enumeration enumeration = msg.getAllHeaders();
        LOGGER.info("Sending mail:");
        while (enumeration.hasMoreElements()) {
            Header header = (Header) enumeration.nextElement();
            LOGGER.info("\t" + header.getName() + " = " + header.getValue());
        }
        LOGGER.info("\t" + "Content-Type" + " = " + msg.getContentType());
        LOGGER.info("\t" + "Encoding" + " = " + msg.getEncoding());

        LOGGER.info(SEPARATOR);
        LOGGER.info(mail.body());
        LOGGER.info(SEPARATOR);

        LOGGER.info(SEPARATOR);
    }

    /**
     * Converts the list of addresses to an array of {@link javax.mail.internet.InternetAddress}.
     *
     * @param addresses the addresses, must not be {@literal null}, but can be empty.
     * @return the array of Internet Addresses, empty if the input list is empty.
     * @throws AddressException if one of the address from 'addresses' is invalid.
     */
    public static InternetAddress[] convert(List<String> addresses) throws AddressException {
        List<InternetAddress> list = new ArrayList<>();
        for (String ad : addresses) {
            list.add(new InternetAddress(ad));
        }
        return list.toArray(new InternetAddress[addresses.size()]);
    }

    /**
     * Type of connections.
     */
    public enum Connection {
        NO_AUTH,
        TLS,
        SSL
    }

}
