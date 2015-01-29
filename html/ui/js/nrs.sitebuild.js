/**
 * @depends {3rdparty/jquery-2.1.0.js}
 */
var NRS = (function(NRS, $, undefined) {

    NRS.loadLockscreenHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("body").prepend(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadHeaderHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("body").prepend(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadSidebarHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("#sidebar").append(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadSidebarContextHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("body").append(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadPageHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("#content").append(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadModalHTML = function(path, options) {
    	jQuery.ajaxSetup({ async: false });
    	$.get(path, '', function (data) { $("body").append(data); });
    	jQuery.ajaxSetup({ async: true });
    }

    NRS.loadPageHTMLTemplates = function(options) {
        //Not used stub, for future use
    }

    NRS.loadModalHTMLTemplates = function(options) {
        jQuery.ajaxSetup({ async: false });
        
        $.get("html/modals/templates.html #secret_phrase_modal_template", '', function (data) {
            var template = Handlebars.compile(data);
            $('div[data-include-modal-template="secret_phrase_modal_template"]').each(function(i) {
                var context = { nr: String(i) };
                $contextTemplate = $(template(context));
                $(this).append($contextTemplate.children().clone());
            });
        });
        
        jQuery.ajaxSetup({ async: true });
    }

return NRS;
}(NRS || {}, jQuery));