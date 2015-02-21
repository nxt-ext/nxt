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

    function _replaceModalHTMLTemplateDiv(data, templateName) {
        var html = $(data).filter('div#' + templateName).html();
        var template = Handlebars.compile(html);
        $('div[data-replace-with-modal-template="' + templateName + '"]').each(function(i) {
            var name = $(this).closest('.modal').attr('id').replace('_modal', '');
            var context = { name: name };
            $(this).replaceWith(template(context));
        });
    }

    NRS.loadModalHTMLTemplates = function(options) {
        jQuery.ajaxSetup({ async: false });
        
        $.get("html/modals/templates.html", '', function (data) {
            _replaceModalHTMLTemplateDiv(data, 'recipient_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'add_message_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'secret_phrase_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_fee_deadline_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_approve_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_rt_hash_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_broadcast_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_note_to_self_template');
        });

        jQuery.ajaxSetup({ async: true });
    }

    function _appendToSidebar(menuHTML, desiredPosition) {
        var inserted = false;
        $.each($('#sidebar_menu > li'), function(key, elem) {
            var compPos = $(elem).data("sidebarPosition");
            if (!inserted && compPos && desiredPosition <= parseInt(compPos)) {
                $(menuHTML).insertBefore(elem);
                inserted = true;
            }
        });
        if (!inserted) {
            $('#sidebar_menu').append(menuHTML);
        }
    }

    NRS.addSimpleSidebarMenuItem = function(options) {
        var menuHTML = '<li id="' + options["id"] + '" class="sm_simple" data-sidebar-position="' + options["desiredPosition"] + '">';
        menuHTML += '<a href="#" data-page="' + options["page"] + '">' + options["titleHTML"] + '</a></li>';
        _appendToSidebar(menuHTML, options["desiredPosition"]);

    }

    NRS.addTreeviewSidebarMenuItem = function(options) {
        var menuHTML = '<li class="treeview" id="' + options["id"] + '" class="sm_treeview" data-sidebar-position="' + options["desiredPosition"] + '">';
        menuHTML += '<a href="#" data-page="' + options["page"] + '">' + options["titleHTML"] + '<i class="fa pull-right fa-angle-right" style="padding-top:3px"></i></a>';
        menuHTML += '<ul class="treeview-menu" style="display: none;"></ul>';
        menuHTML += '</li>';
        _appendToSidebar(menuHTML, options["desiredPosition"]);
    }
    
    NRS.appendMenuItemToTSMenuItem = function(itemId, options) {
        var menuHTML ='<li class="sm_treeview_submenu"><a href="#" ';
        if (options["type"] == 'PAGE' && options["page"]) {
            menuHTML += 'data-page="' + options["page"] + '"';
        } else if (options["type"] == 'MODAL' && options["modalId"]) {
            menuHTML += 'data-toggle="modal" data-target="#' + options["modalId"] + '"';
        } else {
            return false;
        }
        menuHTML += '><i class="fa fa-angle-double-right"></i> ';
        menuHTML += options["titleHTML"] + ' <span class="badge" style="display:none;"></span></a></li>';
        $('#' + itemId + ' ul.treeview-menu').append(menuHTML);
    }

    NRS.appendSubHeaderToTSMenuItem = function(itemId, options) {
        var menuHTML ='<li class="sm_treeview_submenu" style="background-color:#eee;color:#777;padding-top:3px;padding-bottom:3px;">';
        menuHTML += '<span class="sm_sub_header"><span style="display:inline-block;width:20px;">&nbsp;</span> ';
        menuHTML += options["titleHTML"] + ' </span></li>';
        $('#' + itemId + ' ul.treeview-menu').append(menuHTML);
    }


    
    return NRS;
}(NRS || {}, jQuery));