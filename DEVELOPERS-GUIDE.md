----
# DEVELOPERS GUIDE #

----
## How can you contribute? ##

  - create pull requests
  - review pull requests
  - help users on issues
  - ask us, the dev team

----
## Tools and Tips ##
### Usable URLs ###

  - **API** - http://localhost:7876/test
  - **DB Interface** - http://localhost:7876/dbshell
  - **Java Class Browser** - http://localhost:7876/doc
  - **Utilities** - http://localhost:7876/admin.html

### Database ###
  
  - H2 embedded database
  - main database: `nxt_db/`
  - test database: `nxt_test_db/`
  - the database directories do not contain user specific data and can be safely deleted
  - but no need to delete them on upgrade, the DbVersion code takes care of schema updates
  - use the nxt.db framework when accessing the database from your code

----
## Coding Process ##


### Branching Model ###

  - [Vincent Driessen's Branching Model](http://nvie.com/posts/a-successful-git-branching-model/)
  - **tl;dr:**
    - master is release branch
    - develop is maintained by Jean-Luc
    - feature/abc is yours

### Design ###

  - better think twice than code twice
  - communicate with community and us

### Implementation ###

  - Coding Style
    - use the existing code as example for whitespace and indentation
    - the default settings of IntelliJ IDEA are good enough
    - make sure your code fits in
    - use [IDE-Code-Style-Config]
  - Code should be self-documenting and readable, only special cases need comments
  - The "Effective Java" book is a must read
  - Some of the advice in "Clean Code" is good

### Testing ###

  - [to be filled by Lior, kushti]
  - all API calls can be tested manually from the auto-generated http://localhost:7876/test page
  - many tests need blocks to be generated, see the examples how to fake the forging process
  - write your tests against the http API or the public java API, which are relatively stable
  
### Documentation ###

  - API calls should be documented first (because if you don't, you never will)

----
## UI Developers ##


### Where to Look ###

  - index.html: all of the html markup for pages and modals 
  - js/nrs.*.js: corresponding js files for each task the file name displays, one roughly for each page
  - js/nrs.modals.*.js: The modal js (popups) for each set of popups, one for each set of modals
  - any CSS: Bootstrap is used for the design, so changes to CSS rules should be generally avoided
  
### Programming Style ###

  - HTML style
    - Make sure to use the i18n for any text data, internationalization
    - Follow everything else as already set up by Wesley
  - JS style
    - Same as above, just make the code fit into every other part
  - Adding a page
    - Create a new html page markup in index.html, refer to other pages, starts with <div id="NAME-page" class="page">
    - Reference the page with a new link in the menu fixed to the left side of the page, from line 245 to 290 at time of writing
    - Create a corresponding js file in /js directory that handles all page specific scripting.
  - Adding a modal
    - Create a new html modal also in index.html, the modals start around line 1750 at time of writing
    - It is fairly easy to make a modal based upon the information from other modals already created.