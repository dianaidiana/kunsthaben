# Diary of my project (or what I've learnt today):

## 19.08 Authentication: Spring Security, JWT, and how all the pieces talk to each other

This one took a few sessions to actually click, so I'm writing it down properly, end to end, while
it's fresh.

### The big picture first

Before touching any code, the thing that unlocked everything else for me: **the server is
stateless between requests**. It doesn't keep a session, a "who's logged in" list, or a token
anywhere. Every single request has to bring its own proof of identity, a JWT, in the
`Authorization` header, and the server re-checks that proof from scratch, every time, before doing
anything else. Nothing is "remembered" from one request to the next.

There are two completely separate phases to this, and keeping them separate in my head is what
finally made it stick:

1. **Login** (or register): prove who you are once, with a password, get a token back.
2. **Every request after that**: present the token instead of the password, get let in if it's
   valid.

### Phase 1: login, step by step

```
Postman/frontend
  │  POST /auth/login  { email, password }
  ▼
AuthController.login(request)
  ▼
AuthService.login(request)
  │
  │  authenticationManager.authenticate(
  │      new UsernamePasswordAuthenticationToken(email, rawPassword))
  ▼
AuthenticationManager  (I never built this class, Spring Security assembles it for me)
  │
  ├─▶ CustomUserDetailsService.loadUserByUsername(email)
  │       └─▶ UserRepository.findByEmail(email) → my User row
  │       └─▶ wraps it as a UserDetails (email, passwordHash, ROLE_USER)
  │
  └─▶ PasswordEncoder.matches(rawPassword, passwordHash)
          true  → returns a populated Authentication (success)
          false → throws BadCredentialsException

back in AuthService:
  │  if authenticate() threw → catch → throw UnauthorizedException
  │  else: userRepository.findByEmail(email) again, to get the numeric id
  │  jwtService.generateToken(user.getId(), user.getEmail())
  ▼
AuthResponse { token }
  ▼
client receives the token in the response body.
Nothing was saved on the server. The server's job is done.
```

The class that actually decides "is this password right" is `AuthenticationManager`. Spring Security builds one
automatically the first time it's needed, as long as it
can find exactly two beans in my app context:

```java

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

and my own `CustomUserDetailsService`, which is the only class I had to write to teach Spring
Security how to find *my* users at all:

```java

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                                  .orElseThrow(() -> new UsernameNotFoundException(email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();
    }
}
```

Two things I had to get used to here. First, `UserDetailsService` doesn't know anything about
*my* `User` entity, that's exactly why this class exists, it's the translation layer between
Spring Security's generic idea of a user (username, password hash, authorities) and my actual
domain. Second, Spring Security ships its own class called `User` too
(`org.springframework.security.core.userdetails.User`), same simple name as mine, so I write it
fully qualified instead of importing it, to avoid the collision.

Once Spring Security has both beans, it wires them together into something called a
`DaoAuthenticationProvider` behind the scenes, and that's what `AuthenticationManager.authenticate(...)`
actually delegates to. I only had to expose the manager itself as a bean so I could inject it:

```java

@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();
}
```

The `UsernamePasswordAuthenticationToken` I pass into `authenticate(...)` is worth pausing on,
because it shows up twice in this whole system with two different meanings, which confused me at
first. Here, with the two-argument constructor `(email, rawPassword)`, it means "here's an
*attempt* to log in, not yet verified." Later, in the filter (phase 2), the exact same class shows
up again but built differently, and means the opposite: "this is already verified, trust it."

Once `authenticate()` returns successfully (or throws), `AuthService` does one more thing: it
looks the user up *again* by email, just to get the numeric `id`, since `authenticate()`'s result
only carries what `UserDetails` exposes, email and password hash, not the id. Then:

```java
public String generateToken(Long userId, String email) {
    var now = new Date();
    var expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
               .subject(email)
               .claim("userId", userId)
               .issuedAt(now)
               .expiration(expiry)
               .signWith(key)
               .compact();
}
```

That's it, that's the whole token: a signed string carrying `userId`, `email`, when it was issued,
and when it expires. Nothing about the password ends up in it at all, once `matches()` confirmed
it once, the password's job is done.

### Phase 2: calling a protected endpoint

```
Postman/frontend
  │  PUT /user/1   header: Authorization: Bearer <token>   body: {...}
  ▼
Spring Security's filter chain (runs before my controller, on every single request)
  ▼
JwtAuthenticationFilter.doFilterInternal(...)   ← this one I wrote myself
  │  reads request.getHeader("Authorization")
  │  strips "Bearer ", calls jwtService.parseToken(rawToken)
  │      verifies the signature with jwt.secret, checks exp hasn't passed
  │      → returns an AuthPrincipal(id, email) if valid, throws if not
  │  wraps it: new UsernamePasswordAuthenticationToken(principal, null, authorities)
  │      (this is the OTHER meaning of this class: "already authenticated, trust it")
  │  SecurityContextHolder.getContext().setAuthentication(authentication)
  │      lives only for this one request, on this one thread, wiped right after
  │  chain.doFilter(request, response)  → passes control onward
  ▼
Spring Security's authorization check (from my SecurityConfig rules)
  │  is PUT /user/1 in the permitAll list? No.
  │  is SecurityContextHolder populated from the step above? Yes → allowed.
  │  (a missing/invalid token means this step rejects with 403, my controller
  │   is never even reached)
  ▼
DispatcherServlet routes to UserController.update(...)
  │  @AuthenticationPrincipal AuthPrincipal principal
  │      Spring Security reads this straight out of SecurityContextHolder and
  │      hands it to me as a plain method parameter, I never touch the context myself
  │  throwForbiddenIfNotOwned(principal, id)
  │      my own rule, not Spring Security's: does the token's owner match the path?
  │  userService.update(id, updateRequest) → ... → response
  ▼
response goes back. SecurityContextHolder is cleared. Next request starts from zero.
```

`JwtAuthenticationFilter` itself:

```java

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                var principal = jwtService.parseToken(header.substring(7));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
```

The thing I had to understand about `OncePerRequestFilter`: it's a Servlet-level concept, from
underneath Spring itself, guaranteeing this code runs exactly once per incoming request, before
any of my own `@RestController` code sees it. The filter never rejects a request on its own, a
missing or bad token just leaves `SecurityContextHolder` empty, and it's the *next* stage,
`SecurityConfig`'s rules, that actually decides whether that's allowed for this particular path.
Swallowing the exception is deliberate, but I now log it (`logger.warn`), so a genuinely broken
token doesn't disappear silently if something is actually wrong in production.

### SecurityContextHolder, precisely: what it is, and where it actually lives

I kept saying "it sets the context" without being able to answer *where* that context physically
sits, so worth nailing down properly instead of hand-waving it.

`SecurityContextHolder` is a plain static utility class. Calling
`SecurityContextHolder.getContext()` doesn't reach into some server-wide shared object, it reaches
into a **`ThreadLocal`**. A `ThreadLocal` is a Java mechanism for giving each thread its own
private copy of a variable, invisible to every other thread, even though the code accessing it
looks completely ordinary and global. So "setting the context" really means: "store this
`Authentication` in a slot that only *this one thread* can see."

That matters here because of how a Servlet container like Tomcat actually works: an incoming HTTP
request gets handed to one worker thread, picked from a pool, and that same thread runs the entire
request from start to finish, `JwtAuthenticationFilter`, the authorization check, my controller
method, all of it, synchronously, on that one thread. So for the lifetime of a single request,
"the current thread" and "this request" are effectively the same thing. That's the whole reason
`SecurityContextHolder.getContext().setAuthentication(...)` in the filter and
`SecurityContextHolder.getContext().getAuthentication()` later, inside my controller, see the exact
same `Authentication` object, without me ever passing it explicitly. They're not talking to some
shared store, they're both just reading the same thread's own private slot, at different points
in that same thread's execution.

And this is also exactly why it's safe under concurrent requests: if two people hit the API at the
same instant, they get two different worker threads from the pool, so two completely separate
`ThreadLocal` slots. Request A's `SecurityContext` is structurally invisible to request B's thread,
there's no way for one request to accidentally see another's identity.

The "wiped right after" part isn't automatic garbage collection, either. Spring Security has its
own filter earlier in the chain whose whole job is clearing `SecurityContextHolder` in a `finally`
block once the response has been sent. That matters because thread pool threads get *reused*: the
exact same underlying thread that just handled my request will go on to handle a completely
unrelated later request. Without that explicit clearing, the next request picking up that recycled
thread could start out already "authenticated" as the previous request's user, which would be a
real, serious bug. So the cleanup isn't a nicety, it's what makes reusing threads safe at all.

Then, how the `AuthPrincipal` ends up as a controller parameter: `@AuthenticationPrincipal` is
resolved by Spring MVC's own parameter-resolution system, the same general mechanism that already
makes `@PathVariable`, `@RequestParam`, and `@RequestBody` work. Spring Security plugs one more
resolver into it, and that resolver's entire job, when it sees `@AuthenticationPrincipal
AuthPrincipal principal` on a method, is:

```java
SecurityContextHolder.getContext().

getAuthentication().

getPrincipal()
```

cast to whatever type the parameter declares. Since the filter, earlier on this exact same thread,
built the `Authentication` with my `AuthPrincipal` record as its principal
(`new UsernamePasswordAuthenticationToken(principal, null, authorities)`), this call just hands
back that very same object. Nothing is re-parsed, re-verified, or looked up again here, it's the
same in-memory object, read back out of the same `ThreadLocal`, one step later in the same
request's lifetime.

One more precise detail worth having, since I asked "does the 3-argument constructor really make
it *trusted*, or is that just a naming thing": it's a real flag, not just convention.
`UsernamePasswordAuthenticationToken` has two constructors. The two-argument one, `(principal,
credentials)`, used in `AuthService` for a login *attempt*, internally calls
`setAuthenticated(false)`. The three-argument one, `(principal, credentials, authorities)`, used
in the filter, calls `super.setAuthenticated(true)` automatically. That boolean is the actual
thing Spring Security's authorization check reads later to decide whether this request counts as
authenticated at all, it's not inferred from which constructor was used, the constructor is just
what sets it.

So, end to end, correcting my own summary from before: the request arrives and goes through the
filter chain regardless of whether the target path is protected, `JwtAuthenticationFilter` runs
unconditionally and tries to parse a token whenever the header is present, `jwtService.parseToken`
checks both the signature and the expiration inside that one call, a successful parse builds an
*already-authenticated* `UsernamePasswordAuthenticationToken`, that gets stored into this request's
thread-local `SecurityContextHolder` slot, a later stage in the chain checks that slot against
`SecurityConfig`'s rules to decide if this specific path is allowed to proceed, and finally, once
it reaches my controller, `@AuthenticationPrincipal` just reads the same slot back out, on the same
thread, and hands me the `AuthPrincipal` as a plain parameter.

And the config that ties the filter into the rest of Spring Security:

```java

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/user/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/artwork/**", "/user/*/artwork/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/user/{id}", "/category/**", "/media/**", "/support/**").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

`csrf().disable()` is there because CSRF protection defends against a very specific attack that
only applies to cookie/session-based logins, where the browser attaches credentials automatically.
Nothing here is a cookie, the token only ever gets attached because my own client code explicitly
adds it, so that attack doesn't apply. `STATELESS` tells Spring Security not to create an
`HttpSession` at all, matching the "no server-side memory" idea from the top of this entry.

### What Spring Security does for me automatically, versus what I actually built

This was the confusing part for the longest time, so writing it out plainly:

**Automatic, just from adding `spring-boot-starter-security` and the two beans:**

- Locking down *every* endpoint by default the moment the dependency is added, before I write any
  rules at all (a bit alarming the first time, every test went from working to `401` instantly).
- Assembling an `AuthenticationManager`/`DaoAuthenticationProvider` from my `UserDetailsService` +
  `PasswordEncoder` beans, without me writing that wiring myself.
- `SecurityContextHolder` and `@AuthenticationPrincipal`, the mechanism that carries "who is this"
  from the filter all the way into my controller method as a plain parameter.
- The `401` vs `403` distinction I kept tripping over: `401` from the *default* lockdown before any
  `SecurityFilterChain` exists, `403` once my own filter chain exists and an anonymous request hits
  `.anyRequest().authenticated()`, since Spring Security treats "anonymous" as a real, non-null
  `Authentication`, just one that isn't authenticated.

**Nothing built in for, I had to write myself:**

- Anything about JWTs at all. Spring Security has zero opinion on JWT, `JwtService` (signing,
  parsing, expiry) is entirely my own code using the `jjwt` library.
- `JwtAuthenticationFilter` itself, the bridge between "here's a raw header string" and "here's a
  populated `SecurityContext`". Spring Security gives me the slot to plug a filter into
  (`addFilterBefore`), not the filter.
- `CustomUserDetailsService`, the bridge between Spring Security's generic user concept and my
  actual `User` entity.
- Which paths are public and which aren't, that list in `SecurityConfig` is a product decision,
  not something Spring infers.
- The ownership check (`throwForbiddenIfNotOwned`) is entirely mine too, and this was a real
  "aha": Spring Security only answers *"is this request authenticated at all"*, not *"is this
  authenticated user allowed to touch this specific row"*. That second question, does the token's
  owner match the `{id}` in the URL, is regular application logic, sitting in the controller,
  completely outside anything Spring Security itself is aware of.

### Auto-login on register

One small design decision I made along the way: `POST /user/register` now returns a token too,
not just the created user, so signing up logs you in immediately instead of forcing a second
`/auth/login` call right after. `UserController` doesn't build the token itself though, it asks
`AuthService`:

```java
UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
    var user = userService.register(userRequest);
    var token = authService.issueToken(user.getId(), user.getEmail());
    return new UserRegisterResponse(user, token);
}
```

`AuthService.issueToken` is a one-line pass-through to `JwtService`. Felt a bit silly writing a
one-line method at first, but the point isn't the line count, it's that `JwtService` stays a
private implementation detail of the `auth` package. Nothing outside `auth` talks to it directly,
only `AuthService` does, so if how tokens get built ever changes, there's exactly one place that
needs to know.

### localStorage vs cookies: the tradeoff I ended up punting on

Once there's a frontend, the token has to live somewhere in the browser, and I spent a while going
back and forth on this, worth writing down since it's not a "one is right" answer, it's a real
tradeoff either way.

**`localStorage` (plain JS, `localStorage.setItem`/`getItem`, what a Bearer-token API like mine
naturally pairs with):**

- Simple. No CORS-with-credentials setup, no CSRF story to build, works the same for a web
  frontend and a mobile app, since Bearer headers don't care about origins the way cookies do.
- The real cost: anything JavaScript can read, `localStorage` included, is readable by *any*
  script running on the page, not just my own code. If the frontend ever has an XSS bug, someone
  rendering user text as raw HTML instead of escaped text, or a vulnerable dependency, an
  attacker's injected script can just do `localStorage.getItem("token")` and exfiltrate it, one
  line, no special access needed. Once they have it, they can use it from their own machine,
  indefinitely, until it naturally expires.
- Using React or Angular narrows this risk a lot, both auto-escape `{value}`/`{{ value }}` by
  default, which closes off the classic "forgot to escape user input" version of XSS. It doesn't
  eliminate it though: `dangerouslySetInnerHTML` in React, `[innerHTML]` in Angular, and any
  vulnerable third-party dependency all sit outside that automatic protection.

**`HttpOnly` cookies (the security-recommended alternative):**

- The token becomes literally unreadable by JavaScript, `document.cookie` won't show it, so the
  one-line exfiltration above just doesn't work, even under the same XSS bug.
- The cost: cookies get attached automatically by the browser to *any* request to that domain,
  which is exactly the CSRF problem, so this reopens the CSRF protection I deliberately turned off
  in `SecurityConfig`, needs `SameSite`, usually a second JS-readable CSRF cookie echoed back as a
  header. It also needs real CORS configuration with credentials if frontend and backend are on
  different origins, `localStorage`/headers don't care about that at all, cookies very much do.
  And logout stops being free: frontend JS can't clear a cookie it can't read, so a real
  `POST /auth/logout` endpoint becomes necessary, not just "the client forgets the token."

**Where refresh tokens fit into this, since I confused myself on this for a while**: they solve a
completely different problem than storage location does. `localStorage` alone already survives a
page refresh, `localStorage` isn't cleared by reloading, only an in-memory JS variable is. What
`localStorage` alone does *not* solve is the token's own 24-hour expiry, once that passes, the
user is logged out regardless of storage, refresh or not. A refresh token is what lets the app
silently get a new access token instead of forcing a real login again, that's a renewal problem,
not a survival-across-reload problem, the two just happen to share the word "refresh."

I'm not deciding this now, no frontend exists yet to actually test CORS/CSRF behavior against, and
cookie-vs-header is exactly the kind of thing that's nearly impossible to verify meaningfully with
a tool like Postman alone. Written up as its own GitHub issue for when there's a real frontend to
build it against.

### Left for later, on purpose

No refresh tokens yet, so the one 24-hour access token is the whole session, no way to renew it
silently, no real logout either (nothing server-side to revoke, since nothing server-side is kept
at all right now). No cookie-based storage, still Bearer header only. Both written up properly as
GitHub issues to revisit once there's an actual frontend to build and test either against.

## What a unit test can and can't prove about `@Transactional`

Wanted my test to prove that if the S3 delete fails, the database delete doesn't persist either
(rollback). Learned that a plain Mockito unit test literally cannot show this: my test builds the
service with `new ArtworkImageService(repository, artworkService, s3StorageService)` directly,
never through Spring, so `@Transactional`'s proxy is never involved at all. So there's no real
transaction, `repository` is just a mock recording which methods were called on
it.

So `verify(repository).delete(artworkImage2)` after a simulated S3 failure only proves that the
code *called* delete before the S3 call blew up, i.e. that the order of operations is right
(which is exactly what makes the real rollback safe once this runs for real, inside Spring). It
doesn't prove anything actually got undone. To really test "nothing gets persisted when S3 fails"
I'd need a proper `@SpringBootTest` integration test with a real database, letting `@Transactional`
actually do its job.

## 12.08 Wiring S3 into the app

### `S3StorageService`

Built a small class whose only job is talking to S3. `uploadFile(MultipartFile)` builds the object
key from a random `UUID` plus the original file extension — not the original filename — because
two different artists both uploading `image.jpg` would otherwise silently overwrite each other's
file:

```java
public String uploadFile(MultipartFile file) {
    var key = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
    s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(key)
                            .contentType(file.getContentType()).build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
    );
    return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);
}
```

### Rewiring `ArtworkImageService`

Before this, `addImage` just trusted a `url` string sent by the client. Now the endpoint receives
the actual file (`multipart/form-data`), and the flow is: check the artist owns the artwork, check
the image cap, validate the file, upload it through `S3StorageService`, save the URL it hands
back. Also had to build `ArtworkImageController` itself — it didn't exist before, only the
service/repository layer did.

### Bug: `NoSuchBucketException`

First real upload attempt threw `NoSuchBucketException: The specified bucket does not exist`. The
request was reaching AWS fine (so credentials were working), it just couldn't find a bucket by the
name in my `application.properties`. Turned out the name I originally picked was already taken —
S3 bucket names are unique **globally**, across every AWS account in existence, not just mine — so
when I created the bucket, AWS silently suggested a longer, actually-unique name
(`kunsthaben-artwork-images-<account-id>-<region>-<suffix>`), and that's the one that actually got
created. Just had to update the property to match reality.

### Region question

Asked whether it mattered that I picked `eu-north-1` (Stockholm) while being in Austria. Short
answer: not really, at this scale — maybe 10-20ms extra latency versus Frankfurt, imperceptible
for a school project, and if anything Stockholm tends to be slightly *cheaper* than
`eu-central-1`. Region choice only really matters if my own server (the compute) is *also* running
inside AWS in a specific region — then keeping compute and storage in the same region avoids
cross-region latency/cost. Since my app just runs locally and calls out to S3 over the internet
regardless of where I deploy it, that doesn't apply here.

### "Will I need a new bucket if I deploy this online?"

No — this was a genuine misconception I had. A bucket is reachable over HTTPS from literally
anywhere; region only describes where the bytes physically live, not who's allowed to reach them.
My app can run on my laptop, a VPS, wherever, and keep using the exact same bucket the whole time.
The only case where region-matching is worth optimizing for is deploying the compute itself inside
AWS in a specific region — a deliberate choice, not something required just because the app went
"online."

## 11.08 S3 storage setup: IAM, buckets, credentials and the AWS SDK

### What S3 actually is

S3 (Simple Storage Service) is AWS's object storage. A place to store files
("objects") and get a URL back. A **bucket** is a namespace/container for objects; it's not a real
folder system even though the console displays it that way.

### The AWS Free Plan

New AWS accounts get a **Free Plan**: $100 in credit, usable for 185 days (6 months). The
important part: this is a hard cap, not classic pay-as-you-go. If I ever went over the credit
within that window, AWS doesn't charge me, it just shuts the account down (data kept for 90 days,
so I'd have a chance to upgrade and recover it). That's genuinely different from a normal AWS
account with a card attached and no ceiling, and it's the reason I felt comfortable going ahead
with a real AWS account for a school project instead of something else.

### IAM and policies

**IAM** (Identity and Access Management) is AWS's system for controlling *who* can do *what* on
*which* resources. Instead of using my main/root account credentials in the app (full
account access, no boundaries), I created a separate **IAM user** just for this project.

A **policy** is a JSON document you attach to that user, listing exactly which actions are
allowed, on exactly which resources. Mine only grants `s3:PutObject`/`s3:DeleteObject`, scoped to
this one bucket's ARN:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::YOUR-BUCKET-NAME/*"
    }
  ]
}
```

This is the "least privilege" idea: if these specific credentials ever leaked, the damage is
capped at "someone can write/delete objects in this one bucket," not "someone owns my AWS
account."

### Creating the bucket

Picked a region (`eu-north-1`, Stockholm) and a name. Left "Block all public access" on at the
bucket level, and instead added a narrow bucket policy allowing public *read-only* access
(`s3:GetObject` only) — so uploaded images can be viewed via a plain URL in a browser, without
opening up writing/listing/deleting to the whole internet.

### Saving credentials without putting them in the repo

Instead of pasting the access key/secret into `application.properties` (where they could easily
end up committed to git by accident), I saved them as a **named profile** in `~/.aws/credentials`:

```
[kunsthaben]
aws_access_key_id = ...
aws_secret_access_key = ...
```

and pointed the Java code at that profile by name, so the actual secret values never touch the
repo at all:

```java
S3Client.builder().

region(Region.of(region)).

credentialsProvider(ProfileCredentialsProvider.create("kunsthaben")).

build();
```

### pom.xml dependencies

Needed the AWS SDK for Java v2. Added their BOM first (Bill of Materials, a shared version list,
so I don't have to manually pick a compatible version for every single AWS artifact), then the
actual `s3` artifact with no version needed since the BOM supplies it:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.29.10</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### What `S3Client` is

`S3Client` is the actual object my code uses to talk to S3 — every `putObject`/`deleteObject` call
goes through it. Built it once as a Spring `@Bean` in a small `S3Config` class (region + the
profile credentials provider above), so the whole app reuses one client instead of creating a new
one per request.

## 07.08 Concurrency: why `@Transactional` doesn't prevent race conditions

`ArtworkImageService.addImage`'s cap check (`MAX_IMAGES_PER_ARTWORK`) is a classic
check-then-act race: two concurrent requests for the same artwork can each read
`count = 9`, both pass the check before either commits, and both insert — ending up
with 11 images even though the limit is 10.
Although `@Transactional` gives atomicity (all your own writes commit together or not at all) and isolation from
uncommitted changes of other transactions, it does not make
concurrent calls to the same method run one at a time. Two transactions can each read
"count = 9" at the same instant, because neither has committed yet when the other reads.

### Pessimistic write

Pessimistic locking on the `Artwork` row before the count check, e.g. a repository
method annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)`, would force a second
concurrent `addImage` call for the *same* artwork to block until the first transaction
commits (other artworks are unaffected, since the lock is per-row).
I'm skipping this fix for later.

## 06.08 Pagination and Specification

### Pagination

I had `getAllCards()` returning a plain `List<ArtworkCardResponse>`, which means every call was
fetching and returning *everything* that matched, no matter how many rows. That's fine for a demo
with 3 artworks, but the homepage grows with the whole platform's artwork count forever, so I
needed a way to ask for just one "page" of results at a time.

Spring Data already has this built in:

- `Pageable` describes *which* chunk I want (page number, page size, sorting). I build one with
  `PageRequest.of(page, size, sort)`.
- `Page<T>` is what comes back: the items, and also totals (`totalElements`,
  `totalPages`) and whether there's a next page.

Just by adding a `Pageable` parameter to my own repository methods, Spring Data handles the
`LIMIT`/`OFFSET` translation to SQL for me:

```java
Page<Artwork> findAllByDeletedAtIsNullAndSold(boolean sold, Pageable pageable);
```

Worth mentioning: returning `Page<T>` costs **two queries**:
`SELECT ... LIMIT ... OFFSET ...` for the content, plus a separate `SELECT COUNT(*)` to compute the totals. If I don't
need the totals, `Slice<T>` is a cheaper
alternative that skips the count query entirely.

### @PageableDefault

Without `@PageableDefault`, a plain `GET /artwork` with no query params would
come back completely unsorted (Spring Data's own default), so I annotated the controller
parameter to keep newest-first as the default behavior once I removed the hardcoded
`OrderByCreatedAtDesc` from the repository method name:

```java

@GetMapping("/artwork")
Page<ArtworkCardResponse> getAllCards(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return service.getAllCards(pageable);
}
```

### PageImpl

`Page<T>` is just an **interface** — it only declares what a page can *do*
(`getContent()`, `getTotalElements()`, `getTotalPages()`, `hasNext()`, ...). `PageImpl<T>` is Spring Data's own concrete
implementation of that interface.
When I mock the repository
(`when(repository.findAllByDeletedAtIsNullAndSold(...)).thenReturn(...)`), Mockito needs a real
object to hand back, not just an interface.

It has two constructors worth knowing:

- `new PageImpl<>(List.of(...))`: treats the given list as the
  whole result (one page). Good enough since my tests only
  care about verifying the mapped content came through correctly, not exercising real pagination
  math.
- `new PageImpl<>(content, pageable, total)`: the fuller form, letting me simulate "this is page
  2 of 5, out of 47 results total," if I ever need to test pagination boundaries specifically
  (`hasNext()`, `getTotalPages()`), which I'm not doing at the moment.

### Fixing this warning:

```
For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(
pageSerializationMode = VIA_DTO)) or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented
in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.
```

As per this blogpost (Coding Steve): https://stevenpg.com/posts/spring-data-page-impl-serialization-warning/

This warning appears when you’re returning Page<T> objects directly from your REST controllers, and Spring is warning
you that the JSON structure might change between versions.

and the fix:

```java

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}
```

This configuration tells Spring to serialize Page objects using a stable DTO structure instead of the internal PageImpl
structure.

With this configuration, your existing controller methods work exactly the same, but the JSON output will use Spring’s
stable PagedModel format:

```JSON
{
  "content": [
    ...
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### I have no clue:

how to test the content of a endpoint that returns a Page object. For now, I'll just test
the status as the response of the content is actually tested at service.

### Specification

The other big thing today was `Specification<Artwork>` for the filters page. The core idea: one
small method per filter criterion, each returning either a real condition or `null` when that
filter wasn't provided:

```java
public static Specification<Artwork> hasMinPrice(Double minPrice) {
    if (minPrice == null) return Specification.unrestricted();
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
}
```

**Correction (10.08):** I originally wrote `return null;` here, believing
`Specification.where(x).and(y)` treated a `null` Specification as "skip this one". That was true
in older Spring Data versions, but this project is on Spring Data JPA 4.1.0, where
`Specification.and(other)` now does `Assert.notNull(other, "Other specification must not be
null")` — passing `null` throws instead of being skipped. Any filter request leaving a field unset
(so its `hasX` method returned `null`) blew up with `Other specification must not be null`. The
fix is the new `Specification.unrestricted()` static factory (added in 4.0), a no-op specification
meant exactly for this: it always returns a `null` *predicate*, which composition does still treat
as "doesn't contribute" — the null-tolerance moved from the `Specification` reference itself to
the `Predicate` it produces.

`Specification.where(x).and(y)` is what lets me chain a dozen optional filters together and only
the ones actually provided end up in the final `WHERE` clause:

```
Specification.where(isNotDeleted())
        .and(isNotSold())
        .and(hasMinPrice(filter.getMinPrice()))
        .and(hasCategoryIn(filter.getCategoryIds()))
// ...
```

### Case-insensitive search without `ILIKE`

Postgres's `ILIKE` is for case-insensitive search, but plain JPA's
`CriteriaBuilder` doesn't have an `ilike()` method as `ILIKE` isn't standard SQL, it's
Postgres-only. In this case, lowercasing must be done from both sides:

```java
private static Specification<Artwork> matchesKeyword(String word) {
    var pattern = "%" + word.toLowerCase() + "%";
    return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("description")), pattern)
    );
}
```

### Multiple keyword search

A single `LIKE '%red house%'` requires that exact phrase, in that exact order, to appear as one
substring, so what I decided to do for now is to split on whitespace and require each word independently to
appear in title or description, ANDed together:

```
return Arrays.stream(keywords.trim().split("\\s+"))
        .map(ArtworkSpecifications::matchesKeyword)
         .reduce(Specification::and)
         .orElse(null);
```

This solution is not ideal and I would like to implement a filter using PostgreSQL Text Search Types
`tsvector` and `tsquery`. This is much more involved so I'll leave it for later.

### The service side

```java

@Transactional(readOnly = true)
public Page<ArtworkCardResponse> search(ArtworkFilter filter, Pageable pageable) {
    return repository.findAll(ArtworkSpecifications.build(filter), pageable)
                     .map(ArtworkCardResponse::from);
}
```

### The controller side

```java

@GetMapping("/artwork/search")
Page<ArtworkCardResponse> search(
        @ModelAttribute ArtworkFilter filter,
        @PageableDefault(size = 20, sort = "createdAt", direction =
                Sort.Direction.DESC) Pageable pageable) {
    return service.search(filter, pageable);
}
```

`@ModelAttribute` tells Spring: "take all the matching query parameters from this GET request and populate them onto
this object's fields, one by one."
Concretely, with a request like:

`GET /artwork/search?keywords=house&minPrice=50&categoryIds=1&categoryIds=3&framed=true`

Spring creates a new ArtworkFilter (using its no-arg constructor), then for each query param, it calls the matching
setter:
setKeywords("house"), setMinPrice(50.0), setCategoryIds(List.of(1L, 3L)) (repeated params automatically collect into a
List), setFramed(true).
Any field with no matching query param is just left at its default (null, since every field in ArtworkFilter is a
boxed/nullable type), which is exactly what     
ArtworkSpecifications.build(...) needs: null means "this filter wasn't provided, skip it."

### Testing filters

I'm not testing the search method of the service (the one that filters) as it would involve mocking the db with
@DataJpaTest. I'm postponing this for later.

## 05.08 Relationships: owning side, and lazy loading

### 1. Only one side of a relationship is "real" to Hibernate

I have this in `Artwork`:

```java

@OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder ASC")
private List<ArtworkImage> images = new ArrayList<>();
```

and this in `ArtworkImage`:

```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "artwork_id", nullable = false)
private Artwork artwork;
```

I thought since I can navigate both ways in Java (`artwork.getImages()` and
`image.getArtwork()`), both sides were "equal". They're not. In the actual database there is only
**one** column that stores this relationship: `artwork_id`, sitting on the `artwork_images` table.
`artworks` has no column pointing back at all.

So Hibernate needs to know which of my two Java fields it should actually trust when saving. That's
exactly what `@JoinColumn` vs `mappedBy` mean:

- `@JoinColumn` on `ArtworkImage.artwork` → this is the **owning side**. Hibernate writes to
  `artwork_id` based on *this* field.
- `mappedBy = "artwork"` on `Artwork.images` → the **inverse side**. It's just a convenience for
  reading (`SELECT * FROM artwork_images WHERE artwork_id = ?` under the hood), Hibernate ignores it
  when saving.

Which means if I ever write code like this:

```java
artwork.getImages().

add(newImage);
```

it does nothing to persist the relationship, because I never touched `newImage.getArtwork()`.
Hibernate would try to insert the image with `artwork_id = NULL` and the DB would reject it
(`nullable = false`).

The fix is to always keep both sides in sync, and the standard way to not forget is a small helper
method on the parent entity:

```java
public void addImage(ArtworkImage image) {
    images.add(image);
    image.setArtwork(this);
}
```

### 2. Lazy collections need an open DB session to be read

`@OneToMany` collections are lazy by default (unlike `@ManyToOne`, which is eager by default).
Hibernate doesn't put a real list in `artwork.images`,
it puts a placeholder that says "run a query the first time someone actually touches me". For that
trick to work, the database session (Hibernate calls it the "persistence context") has to still be
open at that exact moment. If it's already closed, you get a `LazyInitializationException`.

My `getDetailById`/`getAllCards` methods call `.getImages()` (indirectly, through
`ArtworkDetailResponse.from(...)`/`ArtworkCardResponse.from(...)`) without being `@Transactional`
at all, so by that logic they should already be broken. They're not, because Spring Boot has
`spring.jpa.open-in-view=true` **on by default**, which keeps the session open for the whole HTTP
request instead of closing it right after the method returns. So it "just works", invisibly,
because of a setting I never touched or even knew existed.

If `open-in-view` ever gets turned off, all my reads that touch lazy
collections would suddenly break, for a reason that would be very hard to trace back to one config
line. So the "honest" fix is to say directly, on the method itself, that it needs a session:

```java

@Transactional(readOnly = true)
public Optional<ArtworkDetailResponse> getDetailById(Long id) {
    return repository.findByIdAndDeletedAtIsNull(id)
                     .map(ArtworkDetailResponse::from);
}
```

I already had `@Transactional` imported in `ArtworkService`, but
from `jakarta.transaction`, not Spring's. `readOnly` doesn't exist on that one at all — it's a
Spring-only attribute. Had to switch the import to
`org.springframework.transaction.annotation.Transactional` for `readOnly = true` to even compile.

## 29.07 Testing with Mockito

### Controller test vs service test

Controller test (`UserControllerTest`) verifies
the whole HTTP contract, not the service's internal logic:

```java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class UserControllerTest {

    @MockitoBean
    UserService service;

    @Autowired
    RestTestClient client;
```

Service tests have no Spring context at all, just Mockito. Verifies the service's
own logic in isolation, with no DB and no HTTP involved:

```java

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    UserService service;

    @Mock
    UserRepository repository;

    @BeforeEach
    void setup() {
        service = new UserService(repository);
    }
```

### `when(...)` — stub what a mock returns

```java
when(repository.findByEmail("bob@ross.com")).

thenReturn(Optional.empty());
```

Only matches calls whose arguments are `.equals()` to what was stubbed. This is why request DTOs
without `@EqualsAndHashCode` can break a `when(mock.method(exactObject))` stub if the
real call uses a different (but equal-looking) instance. Using `any()`/`eq()` fixes it. any() tells Mockito "match this
argument no matter what its value is, don't even try .equals().

### `verify(...)` — assert a mock was actually called

For void methods, there's no return value to assert on, so `verify` is the only way to confirm
something happened:

```java
service.delete(1L);

verify(repository).

delete(user);
```

`never()` proves the opposite: that something did *not* happen.

```java
assertThrows(NotFoundException .class, () ->service.

delete(1L));

verify(repository, never()).

delete(any());
```

### `ArgumentCaptor` — inspect what a mock was actually called with

`when`/`verify` only let you control or confirm that a call happened, `ArgumentCaptor` lets you
grab the real object a mock was called with. I needed this to verify password hashing, since the
returned `UserResponse` doesn't expose `passwordHash`. I had to capture the actual `User` passed to
`save(...)`:

```java
void registerSuccessfully() {

    var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123", "Vienna", "1020");
    when(repository.findByEmail("bob@ross.com")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> {
        User savedUser = invocation.getArgument(0);
        savedUser.setId(1L);
        savedUser.setCreatedAt(createdAt);
        return savedUser;
    });

    var result = service.register(request);

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(repository).save(userCaptor.capture());
    var savedUser = userCaptor.getValue();

    assertNotEquals("password123", savedUser.getPasswordHash());
    assertTrue(encoder.matches("password123", savedUser.getPasswordHash()));

    var expectedUserResponse = new UserResponse(1L, "Bob Ross", "bob@ross.com", null, null, "Vienna", "1020", null, createdAt);
    assertEquals(expectedUserResponse, result);
}
```

## 28.07 @Transactional annotation

There are these two ways of doing update:

### fetching, setting and saving:

```java
public UserResponse update(Long id, UserUpdateRequest
        request) {
    var user = repository.findById(
                                 id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

    user.setCity(request.getCity());
    user.setPostcode(request.getPostcode());
    user.setBannerUrl(request.getBannerUrl());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setAbout(request.getAbout());

    var savedUser = repository.save(user);
    return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
            savedUser.getBannerUrl(), savedUser.getAvatarUrl(), savedUser.getCity(),
            savedUser.getPostcode(), savedUser.getAbout(), savedUser.getCreatedAt());

}
```

### using @Transactional, no explicit save():

```java

@Transactional
public UserResponse update(Long id, UserUpdateRequest request) {
    var user = repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

    user.setName(request.getName());
    user.setCity(request.getCity());
    user.setPostcode(request.getPostcode());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setBannerUrl(request.getBannerUrl());
    user.setAbout(request.getAbout());

    return new UserResponse(user.getId(), user.getName(), user.getEmail(),
            user.getBannerUrl(), user.getAvatarUrl(), user.getCity(),
            user.getPostcode(), user.getAbout(), user.getCreatedAt());
}
```

Without @Transactional on the service method, each individual
repository call (findById) runs in its own short-lived transaction managed internally by Spring Data. By the time it
returns, that transaction (and the Hibernate session backing it) has already closed, so the User object you
get back is detached: mutating its fields afterward does nothing until you
explicitly save() it again.

With @Transactional on the whole method, one single transaction spans the entire method body, findById returns a
managed entity that Hibernate is actively tracking for the duration. Any field you mutate on it is             
automatically detected ("dirty checking"), and Hibernate issues the UPDATE on its own when the transaction commits at
the end of the method. No save() call needed at all.

## 27.07 DTOs and validation

### What is a DTO

A DTO (Data Transfer Object) is a plain class that defines exactly what an HTTP request or response body must
contain, separate from the `@Entity` class. The
A DTO is needed when an entity has a sensitive field, a bidirectional
relationship that risks Jackson recursion, needs more than one response shape, or is a join-table
entity (in which case I reuse the *other side's* DTO instead of inventing a new one).

For example in the case `users` case:

```java
public class UserRegisterRequest {

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private final String name;

    @Email(message = ErrorMessages.EMAIL_INVALID)
    private final String email;

    @NotBlank(message = ErrorMessages.PASSWORD_REQUIRED)
    private String password;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;
}
```

```java
public class UserUpdateRequest {

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @URL(message = ErrorMessages.BANNER_URL_INVALID)
    private String bannerUrl;

    @URL(message = ErrorMessages.AVATAR_URL_INVALID)
    private String avatarUrl;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    private String about;
}
```

```java
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String bannerUrl;
    private String avatarUrl;
    private String city;
    private String postcode;
    private String about;
    private OffsetDateTime createdAt;
}
```

`UserResponse` deliberately has no `passwordHash` field. That's the whole reason it exists
instead of just returning `User` directly.

**DTOs I chose to skip**: `Category`, `Media`, `Support`. These are flat, nothing sensitive, and
nothing else has a back-reference to them (no recursion risk), so the controller just returns the
entity directly.

### Adding spring-boot-starter-validation

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### @Valid vs JPA automatic validation

On the DTO, validation only runs because the controller says so:

```java

@PostMapping("/user/register")
UserResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
    return userService.register(userRequest);
}
```

Without `@Valid` here, `@NotBlank`/`@Email` on `UserRegisterRequest` would have no effect.

On the `@Entity`, the exact same kind of annotations get checked automatically, with **no**
`@Valid` anywhere:

```java

@Entity
@Table(name = "users")
public class User {
    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;
    // ...
}
```

This works because Bean Validation integration is part of the JPA spec itself. Hibernate checks
for a Bean Validation provider (Hibernate Validator, which the dependency above brings in) and,
if present, validates the entity automatically right before every insert/update.

### Error handling for validation

There are two different Errors that will be thrown after validation. `MethodArgumentNotValidException.class`
is thrown by Spring MVC itself. It carries a BindingResult with one FieldError per failed constraint
(field name + the message from the annotation, e.g. @NotBlank(message = "...")).

```java

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleSpringMvcValidation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getFieldErrors()
     .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
}
```

`jakarta.validation.ConstraintViolationException.class` is the Entity validation failure (thrown by Hibernate/JPA)

```java 

@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
public ResponseEntity<String> handleEntityValidation(jakarta.validation.ConstraintViolationException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
}
```

## 24.07 How to handle exceptions of the API

```java

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }
}
```

```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

Exception propagation mechanics: nothing "catches" an exception in your controller/service, it's ordinary Java stack
unwinding (only possible because every exception  
here is unchecked) all the way out of your code, and only then does Spring's DispatcherServlet catch it and dispatch to
a matching @ExceptionHandler.

## 22.07: More on making illegal states irrepresentable and Composite keys

### Make Java object instances valid

The same as I did with Frame I should do it with the dimensions of the artwork, so that I can't create an artwork
with negative dimensions, even though I already had a CHECK constraint at db layer. There's a real window between
construction and the eventual save where an Artwork with a negative dimension is a perfectly valid Java object that any
code can read, pass around, or compute with. The bug only surfaces if and when something happens to persist it, which
might be much
later, in a different method, possibly never (if this Artwork is only ever used in memory for some calculation and never
saved).

That's why I created an Dimensions class which I also embed in Artwork and has a factory that validates the integrity of
the dimensions:

```java
public class Dimensions {

    @Column(name = "dim_x", columnDefinition = "NUMERIC(8,2) CHECK (dim_x > 0)", nullable = false)
    private double x;

    @Column(name = "dim_y", columnDefinition = "NUMERIC(8,2) CHECK (dim_y > 0)", nullable = false)
    private double y;

    @Column(name = "dim_z", columnDefinition = "NUMERIC(8,2) CHECK (dim_z > 0)")
    private Double z;

    public static Dimensions of(double x, double y, Double z) {
        if (x <= 0 || y <= 0 || (z != null && z <= 0)) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        return new Dimensions(x, y, z);
    }
}
```

### The primary key class [for a composite key] must be serializable.

By adding implements Serializable, you are just fulfilling a technical requirement of the JPA specification. It
ensures that Hibernate can safely move your composite keys around in memory, caches, or across networks without breaking
the
object. Rule of thumb: Every time you create an @Embeddable class to be used as an @EmbeddedId, always add implements
Serializable.

## 21.07: Normalization overkill & making illegal states irrepresentable

While planning my database schema, I created this table:

```postgresql
CREATE TABLE IF NOT EXISTS artworks
(
    id          SERIAL PRIMARY KEY,
    artist_id   INTEGER        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       TEXT           NOT NULL,
    price       NUMERIC(12, 2) NOT NULL,
    year        INTEGER        NOT NULL,
    description TEXT           NOT NULL,
    city        TEXT           NOT NULL,
    postcode    TEXT           NOT NULL,
    dim_x       NUMERIC(8, 2)  NOT NULL,
    dim_y       NUMERIC(8, 2)  NOT NULL,
    dim_z       NUMERIC(8, 2),
    framed      BOOLEAN        NOT NULL,
    dim_frame_x NUMERIC(8, 2),
    dim_frame_y NUMERIC(8, 2),
    dim_frame_z NUMERIC(8, 2),
    category_id INTEGER        REFERENCES categories (id) ON DELETE SET NULL,
    medium_id   INTEGER        REFERENCES media (id) ON DELETE SET NULL,
    support_id  INTEGER        REFERENCES supports (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMPTZ,
    sold_at     TIMESTAMPTZ,
    reserved    BOOLEAN     DEFAULT false
);
```

I came up with the question: is this violating the make illegal states irrepresentable rule?

Looking at this section of the table:

```postgresql
framed BOOLEAN NOT NULL,
dim_frame_x NUMERIC(8, 2),
dim_frame_y NUMERIC(8, 2),
dim_frame_z NUMERIC(8, 2),
```

what happens if the artwork is framed but no dimensions are given? or if it is unframed but there are frames dimensions
given?
Should I instead create a separate table for framed dimensions and provide a nullable foreign key to the artworks
table?

The short answer is: **Keep them in the same table, but use `CHECK` constraints.**

In database design, moving a few optional columns to a separate table just to avoid `NULLs` is often considered **"
Normalization Overkill."** It makes your queries slower (requires more joins) and your Java code more complex without
providing much benefit.

How to handle **"making illegal states irrepresentable"** effectively:

### 1. The "PostgreSQL Way" (Constraints)

Instead of a separate table, use a `CHECK` constraint. This forces the database to reject "illegal states" (like having
frame dimensions when `framed` is false, or having a frame with no dimensions).

You can add this to your `CREATE TABLE` statement:

```sql
CONSTRAINT check_frame_logic CHECK (
    (framed = FALSE AND dim_frame_x IS NULL AND dim_frame_y IS NULL AND dim_frame_z IS NULL)
    OR
    (framed = TRUE AND dim_frame_x IS NOT NULL AND dim_frame_y IS NOT NULL)
    -- dim_frame_z can stay nullable even if framed
)
```

**Why this is better than a separate table:**

* **Performance:** You get the data in one fetch (no JOIN needed).
* **Safety:** The database literally won't let you save a "broken" artwork record.
* **Atomicity:** You don't have to worry about a transaction failing halfway between the `artworks` table and a `frames`
  table.

---

### 2. The "Spring Boot Way" (`@Embeddable`)

If you want your **Java code** to be cleaner (and follow the "illegal states" principle in your objects), you can use
JPA's `@Embeddable`.

This allows you to have a `Frame` object in Java, but keeps the database table **flat**.

**The Java Class:**

```java

@Embeddable
@Getter
@Setter
public class Frame {
    private boolean framed;
    private Double dimFrameX;
    private Double dimFrameY;
    private Double dimFrameZ;
}
```

**The Entity:**

```java
public class Artwork {
    // ... other fields ...

    @Embedded
    private Frame frame;
}
```

In this way you can make your Constructors protected/private

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
```

and force the creation of Frames to be done by controlled factories:

```java
public static Frame framed(double dimX, double dimY, Double dimZ) {
    if (dimX <= 0 || dimY <= 0 || (dimZ != null && dimZ <= 0)) {
        throw new IllegalArgumentException("Frame dimensions must be positive");
    }
    return new Frame(true, dimX, dimY, dimZ);
}

public static Frame unframed() {
    return new Frame(false, null, null, null);
}
```

