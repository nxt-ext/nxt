----
# PLUGIN DEVELOPERS GUIDE #

----
Current Plugin Version: 1

## Introduction ##

By developing a plugin, you can add functionality to the NRS client. Have
a look at the client source code and documentation to get an overview
about the various javascript APIs and best practices and examples how to
use them.

For a plugin to be valid is has to be delivered with a minimum set of files
and come with a ``manifest.json`` plugin manifest file being compatibel with
the current mayor plugin version and providing some meta information about
the plugin. 

----
## Example Plugin ##

There is an example plugin ``hello_world`` which can be found in the ``plugins``
folder. If you want to see this plugin in the client UI you can activate it
in the associated ``manifest.json`` file.

----
## File Structure ###

The following is the minimal file structure for a plugin:

```
[plugin_name]/
[plugin_name]/manifest.json
[plugin_name]/html/pages/[plugin_name].html
[plugin_name]/html/modals/[plugin_name].html
[plugin_name]/css/[plugin_name].css
```

### Manifest File ###

Meta information about the plugin is provided as a ``JSON`` dictionary in a
``manifest.json`` file in following format:

```
{
    //mandatory
    "pluginVersion": 1, //Integer, don't use parenthesis!
    
    "name": "Name of your plugin", //max. 20 characters
    "myVersion": "Your plugin version", //no format requirements
    "short_description": "A description of your plugin", //max. 200 characters
    "infoUrl": "http://infosaboutmyplugin.info",
    "startPage": "hello_world", //One of the pages used for NRS.pages.PAGENAME method(s)

    "nrsVersion": "1.5.0", //ALWAYS provide three sequence numbers, no additions!

    //optional
    "activated": false, //hard-set plugin activation/deactivation, default: true
    "sidebarOptOut": true //opt out of being listed under sidebar "Plugins" entry, default: false
}
```

Hint: Don't use comments in your own ``JSON`` file!

### Plugin Compatibility/Valdation ###

Plugins are compatible when the manifest file provided is written for the same
mayor plugin version supported by the client where the plugin is installed.

Mayor plugin versions won't change very often, minor plugin version releases will
remain compatible within the mayor version.

When a detected plugin is determined as compatible the NRS client is then validating
manifest file format and file structure.

### NRS Compatibility ###

Due to the broad scope of plugins the functional compatility of the plugin when 
executed with various NRS versions can't be guaranteed by the plugin mechanism 
and is the responsibility of the plugin creator.

the ``nrs_version`` attribute in the manifest file indicates the NRS version
the plugin was written for.

## Best Practices for Development ##

- Namespace your function names, CSS IDs and classes and other possible
identifiers to avoid collisions affecting core NRS behaviour
- Don't manipulate non-plugin HTML or CSS with your javascript code or CSS
declarations

----
## Changelog ##

**Version 1.0, 2015/02/16**

Initial plugin/manifest version









