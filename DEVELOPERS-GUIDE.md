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

### Translation ###

#### Coding ####

Translation is done with the ``i18next`` Javascript translation library. Translations can be added to the code in the following way:

- With ``data-i18n`` data attribute in HTML code, e.g. ``<span data-i18n="send_message">Send Message</span>``
- Via ``$.t()`` function in JS, e.g. ``$.t("send_message")``

Translation files can be found in the ``locales`` folder, base language is the english translation in ``locales/en/translation.json``.

When adding new text/labeling visible in the UI do the following:

- Use one of the methods outlined above, choose an appropriate translation key
- Add both the key and the english text to the top of the english translation file
- Please don't use namespaces in your keys (e.g. not ``namespace.mynewkey``) since this is complicating the filestructure of translation files when created automatically and cause problems when importing files to translation service
- If you later change the english text in the HTML please also change the text within the english translation file, otherwise the new english text is overwritten with the old english text from translation file

#### Process translation files #####

Translation of the client UI to other languages is done by the community within a crowdsourced process on the platform Crowdin:

- https://crowdin.com/project/nxt-ui-translation

For providing new translation strings on the platform for the community to translate do the following:

1. FIRST download the latest translation files from Crowdin (you need permissions for that), to make sure that no new translations are lost, and replace the language folders like ``fa``, ``pt-BR``,... with the folders downloaded. Please make sure to NOT touch the english folder ``en``.
2. Count the rows of the english translation file, e.g. ``wc -l ui/locales/en/translation.json``
2. Parse translation strings not yet included in the english translation file with the i18next parser (extra install) with ``i18next ui -r -l en -s . -o ui/locales/`` (due to a strange bug caused by a file in the ``fonts`` folder ("null bytes in path"), move this folder outside of the ``ui/`` directory for this and move it back afterwards)





