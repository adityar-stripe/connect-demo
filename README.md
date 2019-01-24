# connect-demo

This demo is dependant on ngrok and maven.

Download ngrok here :https://ngrok.com/download

Expose port 4567 using ngrok

``` ngrok http 4567 ```

Use the Stripe dashboard to add the URL ngrok returns as webhook listener. For eg if http://9df6fcb2.ngrok.io is the URL returned, use http://9df6fcb2.ngrok.io/webhook as the webhook URL and listen only to the source.chargeable event


Substitute your private key in App.java.

Substitute your public key in input.js

Install and run using maven

```
mvn clean install
mvn exec:java
```


To connect an account for the first time navigate to /connect . After connecting an account use the base URL(http://9df6fcb2.ngrok.io)  to charge the connected account.
