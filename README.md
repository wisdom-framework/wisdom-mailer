# Wisdom-Mailer

The Wisdom Mailer is an extension to Wisdom allowing application to send mails. A Wisdom extension to send mails
using SMTP. It supports most of the mail services such as gmail.

## Installation

Add the following dependency to your `pom.xml` file:

````
<dependency>
    <groupId>org.wisdom-framework</groupId>
    <artifactId>wisdom-mailer</artifactId>
    <version>${project.version}</version>
</dependency>
````

*Or* copy the jar file to the Wisdom's +Application+ directory.

## Configuration

The configuration can be done either from the +application.conf+ file (+src/main/configuration/application.conf+) or
using system properties. For all the properties described below you can use either way.

| Property               | Description                                                                | Default Value  |
| ---------------------- |:--------------------------------------------------------------------------:| --------------:|
| +mail.smtp.connection+ |the connection type among +NO_AUTH+, +SSL+ and +TLS+                        |+NO_AUTH+       |
| +mail.smtp.host+       |the SMTP server hostname (such as _smtp.gmail.com_)                         |Mock Server     |
| +mail.smtp.port+       |the STMP server port. Default is computed according to the connection type. |25 or 465       |
| +mail.smtp.from+       |the default _from_ address used by the mail sent when it is not explicitly set| No value     |
| +mail.smtp.username+   |the account username when the service requires authentication               | _required_     |
| +mail.smtp.password+   |the account password when the service requires authentication               | _required_     |
| +mail.smtp.debug+      |enables the debug mode dumping lots of information about the mail sending process |false     |

### Examples

The following example is a correct configuration to use gmail:

````
# Mailer configuration
# ~~~~~~~~~~~~~~~~~~~~
mail.smtp.connection = SSL
mail.smtp.host = smtp.gmail.com
mail.smtp.port = 465
mail.smtp.from = username@gmail.com
mail.smtp.username = username@gmail.com
mail.smtp.password = pwd
#mail.smtp.debug = true
````

The username and password can be set using system properties (as well as all other properties). In this case the
+application.conf+ file would contain:

````
# Mailer configuration
# ~~~~~~~~~~~~~~~~~~~~
mail.smtp.connection = SSL
mail.smtp.host = smtp.gmail.com
mail.smtp.port = 465
mail.smtp.from = username@gmail.com
#mail.smtp.username = Not set here
#mail.smtp.password = Not set here
#mail.smtp.debug = true
````

## Usage

Once configured you can use the mailer service as follows:

````
package org.wisdom.mailer;

import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.Controller;
import java.io.File;

@Controller
public class Example extends DefaultController {

    @Requires
    MailSenderService mailer;

    public void sendMail() throws Exception {
        // Send a simple mail
        mailer.send(new Mail()
                .to("me@wisdom-framework.org")
                .subject("Welcome to Wisdom")
                .body("Hello !"));

        // Send a mail with an attachment
        mailer.send(new Mail()
                .to("me@wisdom-framework.org")
                .subject("Wisdom Log")
                .body("Here is the current Wisdom Log file")
                .attach(new File("logs/wisdom.log")));
    }
}
````

The +MailerSenderService+ is injected using the +@Requires+ annotation, then you can use it directly.

