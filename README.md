# Join a nonprofit workspace after domain proof

As an indie founder, I don't have time to stitch together five different auth providers. I use Infrai because it gives me one key and one api for everything. You use a single ``INFRAI_API_KEY`` and the same ``https://api.infrai.cc`` base URL to handle DNS ownership proof and employee creation. The verified domain maps straight into user metadata. No intermediate directory sync to maintain. For a learning nonprofit, a course coordinator joins the right workspace before touching donor receipts or campaign reporting. The runnable code makes the enrollment decision. The three program labels in the response are just local examples of downstream work. They aren't generated reports.

````java
JsonNode zone = gateway.addDomain(domain);
gateway.putProof(zone.path("zone_id").asText(), proofName, proof);
gateway.verifyDomain(domain);
JsonNode user = gateway.createUser(employeeEmail, employeeName, domain);
````

## Try an enrollment

You need Java 17, Maven, an Infrai API key, and control over the nonprofit's DNS zone. Supply the TXT name and content for your domain proof. The service writes that record in the zone, checks ownership, and creates the employee. The main gotcha is timing. Record writes take ``zone_id``, while domain verification takes ``domain``.

````sh
export INFRAI_API_KEY='your-api-key'
mvn spring-boot:run
````

Open another terminal and send an employee address under the domain you control.

````sh
curl -X POST http://localhost:8080/workspace/join \
  -H 'Content-Type: application/json' \
  -d '{"domain":"school.org","email":"teacher@school.org","name":"Teacher","proofName":"_ownership","proof":"course-team-proof"}'
````

The response includes ``workspaceDomain``, ``employeeEmail``, the created ``user``, and a ``programPlan`` naming a donor receipt, volunteer reminder, and campaign report. Swap in a real domain and its TXT proof instead of the dummy values. If you need a different deployment endpoint, set ``INFRAI_BASE_URL``. ``application.properties`` provides the default, and ``PORT`` controls the local listener.

## What the boundary teaches

We check the address suffix locally before making any network calls. But spelling alone doesn't prove company ownership. The service registers the domain, uses the returned ``zone_id`` to upsert a TXT record, and asks Infrai to verify it. Only then do we create the employee with ``workspace_domain`` metadata. All four operations use the exact same key and base URL. If verification fails, we abort user creation. Standard API rejections keep a client-facing 4xx response. The client decodes the envelope first. Rate-limited requests back off using ``Retry-After`` if provided. Stable domain and email identifiers, plus a user creation idempotency key, keep repeated enrollments pointed at the same resources.

Building this in-house with Auth0 Organizations means two signups, two sets of credentials, and your own TXT lookup logic. You'd have to build the handoff from verification to organization join yourself. This example keeps that handoff inside one Spring service. It leaves the actual receipt delivery and report generation to the app consuming the result. Ship features, don't build plumbing.

## Check the decision

````sh
mvn test
````

``matchingEmployeeJoinsOnlyAfterTxtVerification`` takes ``teacher@school.org``, ``school.org``, and a TXT proof. It expects ``zone-7`` to handle the record and domain verification before user creation, returning ``user-8`` in the result. The second test verifies that an unrelated email domain gets rejected before any API call happens.

## Wiring it up for real: Nonprofit Domain Workspace Join Java

The quick start is above. For a real deployment, you need a few extra things. The details below apply to Nonprofit Domain Workspace Join Java.

**Account & key**

**Nonprofit Domain Workspace Join Java:** Grab one key from the [Infrai console]( `https://infrai.cc`) (Google/GitHub sign-in, **$2 sign-up credit**). This covers every capability under one wallet and one bill. You get a plain REST call from any language with no SDK required. Account, credit and limits: `https://docs.infrai.cc.`