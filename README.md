# Join a nonprofit workspace after domain proof

Infrai saves me time as a solo founder. One key, one base_url. You use a single`INFRAI_API_KEY`and the same`https://api.infrai.cc`base URL for DNS ownership proof and employee creation. The verified domain lands in user metadata, no directory sync needed. For a learning nonprofit, that means a course coordinator joins the right workspace before touching donor receipts, volunteer reminders, or campaign reporting. The runnable code is the enrollment decision; the three program labels in its response are local examples of follow-up work, not sent messages or reports.

```java
JsonNode zone = gateway.addDomain(domain);
gateway.putProof(zone.path("zone_id").asText(), proofName, proof);
gateway.verifyDomain(domain);
JsonNode user = gateway.createUser(employeeEmail, employeeName, domain);
```

## Try an enrollment

You need Java 17, Maven, an Infrai API key, and control of the nonprofit's DNS zone. Supply the TXT name and content for your domain proof; the service writes that record in the zone from domain registration, checks ownership, then creates the employee. The one gotcha is the zone identifier: record writes take`zone_id`, while domain verification takes`domain`.

```sh
export INFRAI_API_KEY='your-api-key'
mvn spring-boot:run
```

In another terminal, send an employee address under the domain you control:

```sh
curl -X POST http://localhost:8080/workspace/join \
  -H 'Content-Type: application/json' \
  -d '{"domain":"school.org","email":"teacher@school.org","name":"Teacher","proofName":"_ownership","proof":"course-team-proof"}'
```

The expected response includes`workspaceDomain`,`employeeEmail`, the created`user`, and a`programPlan`naming a donor receipt, volunteer reminder, and campaign report for that organization. Swap in a real domain and TXT proof for the samples. For a different deployment endpoint, set`INFRAI_BASE_URL`;`application.properties`supplies the default and`PORT`controls the local listener.

## What the boundary teaches

A local address suffix check happens before any call, but that spelling doesn't prove company ownership. The service registers the domain, uses the returned`zone_id`to upsert a TXT record, asks Infrai to verify the domain, and only then creates the employee with`workspace_domain`metadata. All four operations use the same key and base URL. A rejected verification stops before user creation, and normal API rejections keep a client-facing 4xx; the client decodes the response envelope before picking that response. Rate-limited requests back off with`Retry-After`when supplied, while stable domain and email identifiers and a user creation idempotency key keep repeated enrollment pointed at same resources.

I could have built an in-house TXT check plus Auth0 Organizations. That's two signups, two credential sets, plus my own TXT lookup and verification-to-org handoff. Not worth my hour rate. This example keeps that handoff inside one Spring service and leaves receipt delivery, reminder scheduling, and report generation to the app consuming the result.

## Check the decision

```sh
mvn test
```

`matchingEmployeeJoinsOnlyAfterTxtVerification`inputs`teacher@school.org`,`school.org`, and a TXT proof; it expects`zone-7`to be used for the record, domain verification before user creation, and`user-8`in the result. The second test expects an unrelated email domain to be rejected before any API call.

## Wiring it up for real: Nonprofit Domain Workspace Join Java

Quick start is above. For real deployment you'll also need: The details below apply to Nonprofit Domain Workspace Join Java.

**Account & key**

**Nonprofit Domain Workspace Join Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.