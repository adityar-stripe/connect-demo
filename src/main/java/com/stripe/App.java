package com.stripe;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import spark.Spark;
import static spark.Spark.*;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import com.stripe.model.*;
import com.stripe.model.oauth.*;
import com.stripe.net.*;
import com.stripe.exception.*;
import com.google.gson.*;

public class App {
    static String[] connected_account_id = new String[1];

    public static void main(String[] args) {
        final Configuration configuration = new Configuration(new Version(2, 3, 0));
        configuration.setClassForTemplateLoading(App.class, "/");
        staticFileLocation("/");
        Stripe.apiKey = "sk_test";
        String platform_id = "ca_id";
        // connected_account_id[0]="acct_1DtCZGLNkyXZMVqQ";
        Spark.get("/", (request, response) -> {

            StringWriter writer = new StringWriter();

            try {
                Template formTemplate = configuration.getTemplate("templates/input.ftl");

                formTemplate.process(null, writer);
            } catch (Exception e) {
                e.printStackTrace();
                Spark.halt(500);
            }

            return writer;
        });

        Spark.post("/charge", (request, response) -> {
            StringWriter writer = new StringWriter();
            try {
                System.out.println(request.queryParams().toString());
                String stripeSourceId = request.queryParams("stripeToken");
                String descriptor = request.queryParams("descriptor");
                String currency="usd";
                int amount = Math.round(Float.valueOf(request.queryParams("amount")) * 100);

                Source cardSource = Source.retrieve(stripeSourceId);
                String redirectUrl = "http://zoho-demo.ngrok.io/3dsredirect";

                // Create a customer and save the source
                Map<String, Object> customerParams = new HashMap<String, Object>();
                customerParams.put("email", "zoho.user@example.com");
                customerParams.put("source", stripeSourceId);
                Customer customer = Customer.create(customerParams);

                // Save customer to connected account
                Map<String, Object> connectedCustomerparams = new HashMap<String, Object>();
                connectedCustomerparams.put("customer", customer.getId());
                connectedCustomerparams.put("original_source", stripeSourceId);
                connectedCustomerparams.put("usage", "reusable");
                RequestOptions requestOptions = RequestOptions.builder().setStripeAccount(connected_account_id[0]).build();
                Source connectedAccountSource = Source.create(connectedCustomerparams,requestOptions);

                // Check if 3DS is required
                if ("required".equalsIgnoreCase(cardSource.getTypeData().get("three_d_secure"))) {
                    // Create 3DS source
                    Map<String, Object> sourceParams = new HashMap<String, Object>();
                    sourceParams.put("amount", amount);
                    sourceParams.put("currency", currency);
                    sourceParams.put("type", "three_d_secure");
                    Map<String, Object> redirectParams = new HashMap<String, Object>();
                    redirectParams.put("return_url", redirectUrl);
                    sourceParams.put("redirect", redirectParams);
                    Map<String, Object> threeDSecureParams = new HashMap<String, Object>();
                    threeDSecureParams.put("card", connectedAccountSource.getId());
                    sourceParams.put("three_d_secure", threeDSecureParams);
                    Source threeDSource = Source.create(sourceParams,requestOptions);
                    response.redirect(threeDSource.getRedirect().getUrl());
                } else {
                    //3DS not required, can charge directly!
                    Charge c = connectedCharge(connectedAccountSource.getId(),amount,currency,connected_account_id[0]);

                    writer.append(c.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Spark.halt(500);
            }

            return writer;
        });

        Spark.post("/webhook", (request, response) -> {

            try {
                String payload = request.body();
                String sigHeader = request.headers("Stripe-Signature");
                String endpointSecret = "whsec_QTq9SL7lIEBynUMPV2IEF2E845PrYGxl";
                Event event = null;
                StringWriter writer = new StringWriter();
                ;

                try {
                    event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

                    if ("source.chargeable".equals(event.getType())) {
                        EventData eventdata = event.getData();
                        Source source = (Source) eventdata.getObject();
                        if ("card".equalsIgnoreCase(source.getType())) {
                            // charge only if 3ds not required
                            if (!"required".equalsIgnoreCase(source.getTypeData().get("three_d_secure"))) {
                                // do nothing
                            } else {
                                // You could charge the card source at this time too!
                                //Charge c = Charge(source);

                            }
                        } else if ("three_d_secure".equalsIgnoreCase(source.getType())) {
                            // This is a charegeable 3DS Source
                            // Go ahead and charge it without waiting for the redirect

                            Charge c = connectedTdsCharge(source,connected_account_id[0]);

                            writer.append(c.toString());

                            System.out.println(source.toJson());
                            System.out.println("charged" + c.toString());

                        }
                    }
                    if ("charge.succeeded".equals(event.getType())) {
                        System.out.println("the charge object is " + event.toJson());
                    }


                } catch (JsonSyntaxException e) {
                    // Invalid payload
                    response.status(400);
                    return "";
                } catch (SignatureVerificationException e) {
                    // Invalid signature
                    response.status(400);
                    return "";
                }
                response.status(200);
                return "";
            } catch (Exception e) {
                e.printStackTrace();
                Spark.halt(500);
            }
            return "";

        });
        Spark.get("/3dsredirect", (request, response) -> {

            StringWriter writer = new StringWriter();

            try {
                request.queryParams();
                String threeDSourceId = request.queryParams("source");
                RequestOptions requestOptions = RequestOptions.builder().setStripeAccount(connected_account_id[0]).build();
                Charge c = connectedTdsCharge(Source.retrieve(threeDSourceId,requestOptions),connected_account_id[0]);
                writer.append(c.toString());
            } catch (Exception e) {
                e.printStackTrace();
                //This is happening because the 3DS Source is already charged in the webhook flow!
                RequestOptions requestOptions = RequestOptions.builder().setStripeAccount(connected_account_id[0]).build();
                writer.append("Charge error. Source is " + Source.retrieve(request.queryParams("source"),requestOptions).toString());
            }

            return writer;

        });
        Spark.get("/refund/:chargeid", (request, response) -> {

            StringWriter writer = new StringWriter();

            try {
                String chargeId = request.params(":chargeid");
                Map<String, Object> refundParams = new HashMap<String, Object>();
                refundParams.put("charge", chargeId);
                RequestOptions requestOptions = RequestOptions.builder().setStripeAccount(connected_account_id[0]).build();
                Refund r = Refund.create(refundParams,requestOptions);
                writer.append(r.toJson());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return writer;

        });

        Spark.get("/connect", (request, response) -> {

            StringWriter writer = new StringWriter();

            try {
                Map<String, Object> map = new HashMap<>();
                map.put("platform_id", platform_id);
                Template formTemplate = configuration.getTemplate("templates/connect.ftl");

                formTemplate.process(map, writer);
            } catch (Exception e) {
                e.printStackTrace();
                Spark.halt(500);
            }

            return writer;
        });

        Spark.get("/connectredirect", (request, response) -> {

            StringWriter writer = new StringWriter();

            try {
                Map<String, Object> tokenParams = new HashMap<>();
                tokenParams.put("grant_type", "authorization_code");
                tokenParams.put("code", request.queryParams("code"));

                TokenResponse resp = OAuth.token(tokenParams, null);
                connected_account_id[0] = resp.getStripeUserId();
                // formTemplate.process(null, writer);
                writer.append("Connected: " + resp.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Spark.halt(500);
            }

            return writer;

        });
    }


    public static Charge connectedCharge(String sourceId,int amount,String currency,String connectedAccountId) {
        Map<String, Object> chargeParams = new HashMap<String, Object>();

        // chargeParams.put("customer", customerId);
        chargeParams.put("amount", amount);
        chargeParams.put("currency", currency);
        chargeParams.put("description", "Sample Description");
        chargeParams.put("source", sourceId);
        chargeParams.put("application_fee", 123);
        // Metadata can be added!
        Map<String, String> initialMetadata = new HashMap<String, String>();
        initialMetadata.put("order_id", "6735");
        chargeParams.put("metadata", initialMetadata);

        // add idempotency key
        String idempotencyKey = String.valueOf(Math.random());

        RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey)
                .setStripeAccount(connectedAccountId)
                .build();

        try {
            return Charge.create(chargeParams, options);
        } catch (StripeException e) {
            e.printStackTrace();
            // Try again with same idempotency key!
            try {
                return Charge.create(chargeParams, options);
            } catch (Exception ee) {
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    public static Charge connectedTdsCharge(Source source,String connectedAccountId) {
        Map<String, Object> chargeParams = new HashMap<String, Object>();

        chargeParams.put("amount", source.getAmount());
        chargeParams.put("currency", source.getCurrency());
        chargeParams.put("description", "Sample Description");
        chargeParams.put("source", source.getId());
        chargeParams.put("application_fee", 123);

        // Metadata can be added!
        Map<String, String> initialMetadata = new HashMap<String, String>();
        initialMetadata.put("order_id", "6735");
        chargeParams.put("metadata", initialMetadata);

        // add idempotency key. can be hash of customer and time etc
        String idempotencyKey = String.valueOf(Math.random());

        RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey)
                .setStripeAccount(connectedAccountId)
                .build();

        try {
            return Charge.create(chargeParams, options);
        } catch (StripeException e) {
            e.printStackTrace();
            // Try again with same idempotency key!
            try {
                return Charge.create(chargeParams, options);
            } catch (Exception ee) {
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


}
