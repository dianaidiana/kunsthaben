# Java Backend Roadmap

This document lists all the things you need to learn in this module, and also some additional optional topics.

It is loosely based on standard developer roadmaps found [here](https://roadmap.sh/), but more tailored to your
knowledge and this course.

- If the topic is a single question, you should be able to give a short (1-3 sentences) answer in your own words. If a
  longer answer is required, it will say so.
- A lot of these questions are very broad. This is on purpose. The goal is not that you dive deep into most of these
  topics - we won't have enough time for that. The goal for many of these questions is that you've heard of this concept
  at least once, and you've understood at least once what it __is__. That doesn't necessarily mean you know how to use
  it. Some topics (mainly related to Spring Boot) are an exception to this. In those cases, the roadmap is more
  detailed, asking you more detailed questions.
- As you can see, for the most part, there are no links/resources provided. This is __on purpose__. The reason for this
  is that in a real job, you will most likely just get access to a completely foreign code base, and you will have to
  research on your own what it all means.
- Generally, you need to finish all the mandatory topics first, before you start on the optional ones. However, we might
  make some exceptions in certain cases - this is something we can discuss in the sprint review.
- This document will definitely change. We might add important topics that were forgotten, or expand on certain topics
  that were not well explained. Or we might adjust the existing topics a little bit to clarify things. Whenever we make
  changes, we'll let you know, and you can look at the commit history of this repo to see what exactly was changed.
- When searching for resources online, we recommend searching __for your specific problem__ first. Search for what
  you're trying to accomplish, or search for the specific error that you're getting.
- The official Spring Boot documentation is often useful. When looking at the Spring Docs, you'll sometimes find
  instructions for imperative (Servlet) applications, and for reactive (WebFlux) applications. You want to look at
  imperative/Servlet, this is the one we're generally using, since it is more widespread. Generally, you can ignore
  anything related to reactive/WebFlux.

## Mandatory Topics

- [ ] How does the web work? Frontend vs Backend
    - [ ] What happens when you open a website in your browser?
    - [ ] What's the difference between frontend and backend?
    - [ ] What is HTML/CSS/JavaScript?
    - [ ] What is a URL?
    - [ ] What is an IP address?
    - [ ] What is localhost/127.0.0.1?
    - [ ] What is the difference between a local IP (Local-Area-Network/LAN) and a global IP (Wide-Area-Network/WAN)?
    - [ ] In one sentence, what's IPv4 and IPv6?
    - [ ] What is a port?
    - [ ] In one sentence, what is DNS?
- [ ] HTTP/REST
    - [ ] What is a network protocol?
    - [ ] What is HTTP?
    - [ ] What are the different parts of an HTTP request/response?
        - [ ] What are the possible verbs/methods of an HTTP request? When would you use each?
        - [ ] What are the request/response headers of an HTTP request? What are some commonly used headers?
        - [ ] What is the body of an HTTP request/response? What data does it usually contain?
    - [ ] What is JSON?
        - [ ] What are the different allowed data types in JSON?
        - [ ] How are arrays and dictionaries represented in JSON?
        - [ ] How are dates (date and time) often represented in JSON?
        - [ ] What does 'serialization' and 'deserialization' mean?
    - [ ] What is curl? What is Postman?
    - [ ] In the context of the web, what is a (REST) API (note that the term "API" has many different meanings, so be a
      bit careful when searching)?
        - [ ] What is REST?
            - [ ] [Link](https://www.codecademy.com/article/what-is-rest)
    - [ ] Practice making HTTP requests with the GitHub API
        - [ ] You will need
          to [generate a personal access token (classic)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token#creating-a-personal-access-token-classic)
          for this to work
        - [ ] Note: Github offers its own tools (the `gh` command, and `octokit`) to access the API more easily. **Do
          not** use this tool for this exercise - the purpose of this exercise is to learn about the more general tools
          which can be used with __any__ API, not just GitHub.
        - [ ] Do these things both with `curl`, with Postman, and later, once we've learned it, in Java (for example
          with RestTemplate)
        - [ ] Make a few GET requests to the GitHub API. For example, list all the repositories for your own account (
          including any private ones, for which you need the personal access token, see above).
        - [ ] Make a POST request to the GitHub API. For example, you could create a new repository.
        - [ ] Make a PATCH request to the GitHub API. For example, you could update and change the name of the
          repository you just created. **Careful**, any change you make this way is permanent!
        - [ ] Make a DELETE request to the GitHub API. For example, you could delete the newly created repository. *
          *Careful**, anything you delete this way is permanently deleted!
    - [ ] What is CORS? Why is CORS disabled for many APIs?
        - [ ] Who decides whether CORS is enabled? The server ("the API"), or the client (you, via Postman/curl/the web
          browser)?
        - [ ] Are the CORS headers part of the HTTP request, or the response?
        - [ ] When making a request, how can you know whether or not CORS is enabled?
        - [ ] Does CORS also apply when you're making requests via Postman/curl/Java? Why/why not?
        - [ ] (Optional) Using JavaScript in the browser (for example using the `fetch` function), perform a request to
          an API that does **not** support CORS (for example, the Twitter API). See what happens in the browser console.
            - [ ] Is there anything you can do to solve this issue? How would you solve this problem if you needed data
              from Twitter in your application?
    - [ ] What's the difference between a Single Page Application (SPA) and Server-Side Rendering (SSR)?
        - [ ] What are the advantages/disadvantages of the two approaches?
        - [ ] If you're developing an SPA, are you working mostly in the frontend, or the backend?
        - [ ] What are some of the most popular SPA frameworks? Which language are they written in?
    - [ ] What is TLS/SSL?
- [ ] Spring Boot
    - [ ] Basics
        - [ ] Project Setup
            - [ ] Use the [Spring Initializr](https://start.spring.io/) to create a new Spring Boot project with the
              following settings:
                - [ ] Project/Dependency Manager: Maven
                - [ ] Spring Boot version: Just select the highest stable (non-snapshot) version
                - [ ] Group: io.everyonecodes
                - [ ] Artifact: spring-module
                - [ ] Java version: 21
                - [ ] Also, add some dependencies:
                    - [ ] Lombok
                    - [ ] Spring Boot DevTools
                    - [ ] Spring Web
            - [ ] Before clicking "Generate", click "Explore" first. Observe what changes in the `pom.xml` file that
              Spring Initializr generates when you add or remove dependencies. Adding or removing dependencies simply
              adds/removes them from the generated `pom.xml` file.
            - [ ] Click "Generate" and unzip the downloaded zip file
            - [ ] Open the project by dragging it onto the IntelliJ icon
            - [ ] Wait until IntelliJ has downloaded all maven dependencies for you
            - [ ] Run the project by finding the generated class containing the `@SpringBootApplication`
            - [ ] Go to `localhost:8080` in your browser. If you see the message `Whitelabel Error Page`, you were
              successful, your server is running!
            - [ ] Maven
                - [ ] What is a library (in the context of Software Development)?
                - [ ] What is a framework (in the context of Software Development)? What's the difference to a library?
                - [ ] What is a package/dependency?
                - [ ] What is a package manager/dependency manager? Why is it useful?
                - [ ] What are the names of the two main package managers for Java?
                - [ ] If you have a project that uses maven:
                    - [ ] What is the `pom.xml` file?
                    - [ ] How would you add a new dependency?
                    - [ ] How would you remove a dependency?
                    - [ ] After adding/removing a dependency, what do you have to do in IntelliJ so that maven updates
                      the dependencies?
                    - [ ] Make sure that maven also downloads the dependencies' documentation in Intellij (see
                      this [link](https://stackoverflow.com/questions/31072667/how-to-force-intellij-to-download-javadocs-with-maven))
        - [ ] Serving some HTML pages
            - [ ] What is a templating engine, like ThymeLeaf? What does it do? Why is it useful?
            - [ ] Add Thymeleaf as a dependency to your maven project (Google it if you don't know how)
            - [ ] Add an `index.html` file containing some html markup to your `resources/templates` folder
                - [ ] Re-run the project and re-load the index page in the browser, to make sure your html file is
                  served correctly
            - [ ] Add another html file and add a Spring `@Controller` class. Make sure the other html file is served at
              the route `localhost:8080/example`. Use `@GetMapping` for this.
            - [ ] Create a css folder in the `resources/static` folder. Add a CSS file with at least one simple change (
              like font or font size). Make sure the CSS file is loaded in at least one of your HTML files. You might
              have to restart the server after each change you make.
        - [ ] Spring basics, MVC architecture
            - [ ] Remind yourself: What is dependency injection (without Spring, in plain Java)?
                - [ ] Write a simple Java service class that uses dependency injection to get injected into other
                  classes (of course you'll also need an interface for this). If possible, try to come up with your own
                  example.
            - [ ] What's the basic idea behind Spring?
                - [ ] What's a Spring Bean?
                - [ ] What's the Spring Application Context?
            - [ ] How does Spring use dependency injection to inject Beans via constructor?
            - [ ] What does the `@Autowired` annotation do? Is it needed for constructor injection? How else can it be
              used?
            - [ ] Simple `@RestController` without database
                - [ ] Create a simple data class
                - [ ] For example, something representing a blog post `Post`, with a title, content, publishedDate,
                  isDraft
                - [ ] Use Lombok annotations to auto-generate the getters and setters
                    - [ ] What is Lombok? Why is it useful?
                - [ ] Create a simple `@RestController` that stores a list of this class as a private property
                - [ ] Add `@GetMapping` and `@PostMapping` endpoints to make it possible to create a post, and to read
                  all post
                - [ ] Test your code by sending requests using Postman
            - [ ] A simple, hand-written in-memory database, with a RestController
                - [ ] Building from the previous example, add an `id` property to the data class you created
                - [ ] Create a new class `PostRepository` with the `@Repository` annotation, and move the list of posts
                  in your `@RestController` to the new class. In your `@RestController` make sure that the repository is
                  auto-injected by Spring
                - [ ] In your repository class, write the following methods:
                    - [ ] `public List<Post> findAll()`
                    - [ ] `public Optional<Post> findById(Long id)`
                    - [ ] `public void deleteById(Long id)`
                    - [ ] `public Post save(Post newPost)`
                        - [ ] If a post with the same `id` as `newPost`, already exists in the list, it should simply be
                          replaced
                        - [ ] If `newPost` does not yet exist, it should be created and added to the list, with an
                          auto-generated `id`
                - [ ] In your `@RestController`
                    - [ ] Update your existing endpoints to use the newly created repository
                    - [ ] Use the `@PathVariable` annotation to get the id for the following two tasks
                        - [ ] Add an `@PutMapping` endpoint to update an existing post or create a new one if it doesn't
                          exist yet
                        - [ ] Add an `@DeleteMapping` endpoint to delete posts
            - [ ] More templating
                - [ ] Create a `WebController` class and add the `@Controller` annotation. This class will serves your
                  HTML files via Thymeleaf templates.
                - [ ] Make sure your `PostRepository` is auto-injected by Spring into your previously created
                  `WebController`
                - [ ] Change the method that returns your index page so that it returns a `ModelAndView`, a built-in
                  class. The `view` should remain your template file (`index.html`), but as the model, you now add the
                  list of all blog posts, as returned by your repository's `findAll` method.
                - [ ] Edit your template in `index.html` so that it displays all blog posts. You can use the `th:block`,
                  `th:each`, `th:include` and `th:text` attributes (search for them on the web to see some examples)
                - [ ] Optionally, add a stylesheet to make your site look a little nicer
            - [ ] Spring layered architecture
                - [ ] What are the different layers in the layered architecture? Give an example for each and explain
                  what they do (give an example for a typical method they might implement).
                - [ ] Model-View-Controller architecture (MVC)
                    - [ ] What does the Spring MVC architecture usually look like?
                        - [ ] Give a concrete example for a Model class
                        - [ ] Give a concrete example for a Controller class
                        - [ ] Give a concrete example for a View
                    - [ ] What's a Spring `@Component`?
                        - [ ] What's a Spring `@Controller/@Service/@Repository`? What's the difference between them?
                - [ ] How does MVC fit into the layered architecture?
            - [ ] Adding a service class
                - [ ] Add a service layer class `PostService` (with the `@Service` annotation).
                    - [ ] Make sure the `PostRepository` you created earlier is auto-injected via Spring
                    - [ ] In the `PostService`, add the following method
                        ```    
                        // Returns all published posts (isDraft == false and publication date before the current date), sorted descending by publication date
                            public List<Post> allPublished() {
                        ```
                    - [ ] In your `WebController`, replace the repository with the `PostService`, and use the
                      `allPublished` method to only return the __published__ blog posts in your index endpoint
            - [ ] What is the difference between a Monolith web application and Microservices?
    - [ ] Testing/Mocking
        - [ ] Mockito
            - [ ] What is mocking, why is it useful?
            - [ ] What is Mockito?
            - [ ] What does `when`/`then` in Mockito do? Why is it useful?
            - [ ] What does `verify`/`verifyNoMoreInteractions` in Mockito do? Why is it useful?
            - [ ] What does the `@MockBean` and `@SpyBean` annotation do in Mockito? Why are they useful? What's the
              difference between the two?
        - [ ] What does the `RestTemplate` class do? Why is it useful? Also check out the newer version, `RestClient`.
        - [ ] What is the `TestRestTemplate` class and how does it relate to the `RestTemplate` class? What's the
          difference between the two? When would you use `RestTemplate`, when `TestRestTemplate`? Also check out the
          newer version, `RestTestClient`.
        - [ ] What do the `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` and
          `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)` annotations do?
        - [ ] What's the difference between a unit test and an integration test? This question is quite difficult to
          answer in general, and you'll find different opinions. It might help if you search specifically in the context
          of Spring Boot.
        - [ ] Write a few unit tests for your service/controller classes
        - [ ] Write a few integration tests for one of your controllers
- [ ] Databases & SQL
    - [ ] What is a database?
    - [ ] What is a relational database?
    - [ ] What is SQL?
    - [ ] What's the difference between SQL and MySQL?
    - [ ] What are some examples for SQL databases?
    - [ ] What are some examples for NoSQL databases?
    - [ ] How is data stored in a relational database?
    - [ ] What is a database schema?
    - [ ] What is a primary key? Where and why is it needed/useful? Give a real-life example
    - [ ] What is the difference between 1-to-1, 1-to-many, and many-to-many relationships? Give a real-life example for
      each of them
    - [ ] What is an integrity constraint? Give a real-life example
    - [ ] What is an ER diagram?
    - [ ] Playing with PostgreSQL
        - [ ] [Download](https://www.postgresql.org/download/), install and run the latest PostgreSQL
        - [ ] Make sure you can connect to the Database. After the installation, it will have created a default user and
          password. Search the web to figure out what it is, and how to connect to the database, so you can run SQL
          queries.
        - [ ] Experiment and run some SQL queries to create some tables and insert some data, to make sure everything's
          working. You can either use the terminal based `pgsql` command or the web-based `pgAdmin` app to run the
          queries. Try both to see which one is easier to use for you.
    - [ ] Database modeling
        - [ ] Model the following situation. Write SQL queries to build the database schema.
        - [ ] There are blog posts. Each blog post has a title and some text content. Blog posts are published at a
          certain time and date. Also, some blog posts are stored as a draft, so they're not published yet.
        - [ ] Every blog post can have multiple authors, and every author can have written multiple posts. An author has
          a first and last name, as well as a (unique) email address and a password.
        - [ ] (Optional, if it helps you) Create an ER diagram that models this database schema. It's probably easiest
          to do this on paper
        - [ ] Write the SQL queries to create the schema
        - [ ] Insert some test data. Insert at least 3 blog posts and 3 authors. Read the following tasks first to
          insert data that is relevant to those tasks.
        - [ ] Write the following queries:
            - [ ] A query that returns all published posts. A post is published if the stored publication date is before
              the current date (you can hardcode the current date in the query or use the `now()` SQL builtin function),
              and also it is __not__ a draft.
            - [ ] (Optional, difficult, requires joins) Return all the titles and content for the posts of a specific
              author, given that author's email address
        - [ ] Update the content of a blog post
        - [ ] Delete a blog post
        - [ ] What is soft deletion? Why can it be useful?
    - [ ] Security
        - [ ] What is an SQL injection attack? How is it performed?
        - [ ] (Optional): Write a query that includes an SQL injection attack to drop a table, and execute it on your
          database
        - [ ] (Optional): How do you prevent SQL injection attacks? Hint: Look up the term "prepared statement". What's
          the difference between a prepared statement and simple string concatenation when constructing the query?
- [ ] JPA
    - [ ] What's an ORM? Why is it useful?
    - [ ] What does JPA stand for? What is it? What is Hibernate? (Hint: The difference between Hibernate and JPA is
      very subtle, you can ignore it. The important thing is that you understand what it does, and why it's useful.)
    - [ ] Adding a database to your project
        - [ ] Add the `spring-boot-starter-data-jpa` and `postgres` dependencies to your project
        - [ ] Edit your `application.properties` file so that Spring connects to your local DB
        - [ ] Convert your `Post` data class to a JPA entity
            - [ ] Make sure the post content and title can have any length when stored in the DB
            - [ ] You'll need at least the following annotations: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`
        - [ ] Rewrite your `PostRepository` so that it is an interface extending the `JpaRepository` interface built
          into Spring
        - [ ] Rewrite your `PostService` so that it doesn't use the `findAll` method of the repository, but instead uses
          the [Spring Repository Query Methods](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods)
          to access the correct data
            - [ ] Why is this important? Why should we not just `findAll` and then filter the data using streams?
        - [ ] Manually test your application (using Postman or the browser) to make sure everything works
        - [ ] Create a second entity, for example a `User`, and add a relationship between blog posts and users with JPA
        - [ ] What does the `@Transactional` annotation do? Where should you add it? In which situations?
        - [ ] What's `FetchType.LAZY` vs `FetchType.EAGER`? When should you use which?
    - [ ] What's a Data Transfer Object (DTO)? Why is it useful? When would you use it?

## Optional Topics

- [ ] General Programming Knowledge
    - [ ] Java generics
        - [ ] What are generics in Java?
        - [ ] How can you write a generic method in Java? Why is this useful?
            - [ ] Remember when we tried to recreate the `filter` method in the topic week for `Streams`? Find the
              method we wrote back then and make it generic (so that it accepts lists of any type)
    - [ ] Concurrency
        - [ ] In programming, what is a thread? Why are threads useful?
        - [ ] What's the difference between a process and a thread?
        - [ ] How can you create a new thread in Java?
        - [ ] What does it mean for an HTTP request to be performed synchronously/asynchronously? What's the difference
          between the two? What are the advantages/disadvantages between the two?
        - [ ] Are the requests performed by RestTemplate/TestRestTemplate performed synchronously or asynchronously?
        - [ ] What is concurrency? What is parallelism? What's the difference between the two?
    - [ ] More git
        - [ ] What is a branch in git? Why are they useful?
        - [ ] You should now how to create, rename and delete local branches
        - [ ] You should know how to push and pull remote branches and what's the difference to a local branch
        - [ ] What is a merge in git?
        - [ ] What is a rebase in git? What's the difference to a merge?
        - [ ] What happens when there is a merge conflict? How can you resolve it?
        - [ ] In GitHub, what is a merge request? How can you create one?
    - [ ] CI/CD
        - [ ] What is Continuous Integration/Continuous delivery? Why is it useful?
- [ ] More on databases/JPA
    - [ ] General database knowledge
        - [ ] What is database normalization?
        - [ ] What are SQL joins? Why are they useful? What different types of SQL joins are there? In what situations
          are they useful?
        - [ ] In the context of databases, what does ACID stand for? What does each of these ideas/concepts mean?
        - [ ] What are NoSQL databases (for example MongoDB)?
            - [ ] Replace the database of your project with a MongoDB database
    - [ ] Relationships with JPA
        - [ ] Create entities with `@OneToMany` and `@ManyToMany` relationships
            - [ ] Take a look at the `@JoinTable` annotation
            - [ ] In this annotation: `@ManyToMany(fetch = FetchType.EAGER)` What does `FetchType.EAGER` mean?
            - [ ] Make sure you understand how JPA translates these annotations to database tables
- [ ] Security
    - [ ] What is the concept of Authentication? What is the concept of Authorization? (Both not necessarily related to
      Spring, but in general.) What is the difference between the two?
    - [ ] What is HTTP basic authentication? How would you send an HTTP request using Postman that includes HTTP basic
      auth credentials (username+password)?
    - [ ] Spring Security Basics
        - [ ] Add the Spring Security maven dependency to your project (research and find the correct maven dependency
          on your own)
        - [ ] In Spring, what does the `@Configuration` annotation do? What does the `@Bean` annotation do?
            - [ ] Be aware that you can also add Configurations to Spring with XML files. This is generally not
              something we recommend doing anymore. Nowadays it's much better to use the Java-based configuration. Some
              tutorials will still use the XML-style configuration, in which case we recommend that you figure out how
              to translate that configuration to Java, if you want to use it.
        - [ ] Go through [this tutorial](https://www.baeldung.com/spring-security-basic-authentication), and create a
          secured endpoint in your Spring application.
        - [ ] Read up a bit on
          the [architecture of Spring Boot Security](https://docs.spring.io/spring-security/reference/5.8/servlet/architecture.html).
          You will probably not understand a lot, since it is extremely complex. The main goal is that you understand
          what a `SecurityFilterChain` is, and how it works.
        - [ ] How can you exclude certain files/endpoints from the `SecurityFilterChain` (for example, if you want them
          to be accessible even by users that are not logged in)?
        - [ ] What is a `PasswordEncoder`? What does it do? Why/When is it needed?
        - [ ] What is a `UserDetailsService`? What does it do? Why/When is it needed?
            - [ ] Hint: Set up an `InMemoryUserDetailsManager` to store the user details in memory first. Hooking this
              up with the Database is difficult and a separate point further down in the roadmap
        - [ ] What are roles in the context of Spring Security?
        - [ ] What does `@EnableGlobalMethodSecurity(securedEnabled = true)` do? What does the `@Secured` annotation do?
            - [ ] Add the `@Secured` annotation to one of your controller or service classes and make sure it works (you
              can test it using Postman or by writing an integration test)
        - [ ] How can you exclude the SecurityConfiguration from your unit tests, so that they still succeed?
            - [ ] Hint: There are two ways to do this, one involves using Mockito's `@MockBean` and the other one uses
              the `@Profile` and `@ActiveProfiles` annotations. The second method is probably more reliable, but also a
              bit more difficult to understand.
    - [ ] Spring Security + Databases
        - [ ] How can you store user credentials (username/password, roles) in the database? Hint: You can use
          `UserDetailsService` to do this, also research the `UserDetails` interface.
            - [ ] Create a `User` class, which should be a JPA entity and store the credentials in the Database.
            - [ ] Why should you create a separate class to implement the `UserDetails` interface? Why should the `User`
              class not implement it directly?
            - [ ] How can you exclude certain properties from JSON serialization?
                - [ ] Hint: Research the `@JsonIgnore` and `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`
                  annotations
            - [ ] Make sure that your application allows you to create users with username, password and roles, store
              them in the DB, and perform authenticated requests with those users
    - [ ] What are cookies? What is a session cookie?
    - [ ] What is CORS?
    - [ ] What is CSRF?
- [ ] Containerization
    - [ ] Docker
        - [ ] What is docker? Why is it useful?
        - [ ] If you're running Windows: How does Docker work on Windows? What is WSL/WSL2? Why is it recommended to
          install WSL2 to work with Docker on Windows?
        - [ ] Install Docker on your system
        - [ ] What command can you use to check your docker version?
        - [ ] What is a docker image? What is a docker container?
        - [ ] How can you see all currently running containers on your system? How can you see __all__ containers (even
          the ones currently not running) on your system?
        - [ ] How can you create and run a docker container from a pre-existing image?
        - [ ] How can you stop/delete a container?
        - [ ] How can you delete an image?
        - [ ] What is a Dockerfile?
        - [ ] (Difficult) Create a Dockerfile for your project. Run a dockerized version of your Spring Boot application
          in a container
            - [ ] If you try this, do it with a stateless app (without a DB or only in-memory data) first, which is much
              easier
    - [ ] Container Orchestration
        - [ ] What is Docker Compose? What is Kubernetes? Why are they useful?
- [ ] DevOps basics
    - [ ] What is Amazon AWS/Google Cloud/Microsoft Azure? What's the difference between using one of these cloud
      services vs. purchasing a computer and manually managing a server?
    - [ ] Look up the following services on Amazon AWS and find out what they do/why they are useful. Find out similar
      services on the other major cloud platforms (Google Cloud/Azure):
        - [ ] EC2 (Cloud Virtual Machines)
        - [ ] S3 (File storage)
        - [ ] Route53 (DNS)
    - [ ] What is a CDN? Why is it useful?
        - [ ] Give some examples for common CDN services
    - [ ] What does "Infrastructure as code" mean? Why is this concept useful? Look up tools like Terraform, Chef,
      Ansible, Puppet and try to understand on a high level what they do.
- [ ] More Frontend
    - [ ] Learn more about JavaScript. I recommend starting with TypeScript, which is a typed version of JavaScript,
      much easier to learn and more in demand. Also, if you learn TypeScript, you also know JavaScript automatically.
    - [ ] Learn more about CSS. Design a website using a design tool (I recommend Figma), or find a nice looking website
      online. Then try to re-create that design perfectly using CSS.
    - [ ] Learn more about Frontend frameworks. A good place to start is React, since it's the most popular one. Look up
      a tutorial and build a simple React app