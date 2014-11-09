/**
 * JS initialization and core functions for API test servlet
 *
 * @depends {3rdparty/jquery.js}
 * @depends {3rdparty/bootstrap.js}
 * @depends {3rdparty/highlight.pack.js}
 */

var ATS = (function(ATS, $, undefined) {
    ATS.apiCalls = [];
    ATS.selectedApiCalls = [];

    ATS.init = function() {
        hljs.initHighlightingOnLoad();
        
        ATS.selectedApiCalls = ATS.setSelectedApiCalls();
        ATS.selectedApiCallsChange();

        $('#search').keyup(function(e) {
            if (e.keyCode == 13) {
                ATS.performSearch($(this).val());
            }
        });

        $(".collapse-link").click(function(event) {
            event.preventDefault();    
        });
        
        $('#navi-show-open').click(function(e) {
            $('.api-call-All').each(function() {
                if($(this).find('.panel-collapse.in').length != 0) {
                    $(this).show();
                } else {
                    $(this).hide();
                }
            });
            $('#navi-show-all').css('font-weight', 'normal');
            $(this).css('font-weight', 'bold');
            e.preventDefault();
        });

        $('#navi-show-all').click(function(e) {
            $('.api-call-All').show();
            $('#navi-show-open').css('font-weight', 'normal');
            $(this).css('font-weight', 'bold');
            e.preventDefault();
        });

        $('.api-call-sel-ALL').change(function() {
            if($(this).is(":checked")) {
                ATS.addToSelected($(this));
            } else {
                ATS.removeFromSelected($(this));
            }
        });

        $('#navi-select-all-d-add-btn').click(function(e) {
            $.each($('.api-call-sel-ALL:visible:not(:checked)'), function(key, value) {
                ATS.addToSelected($(value));
            });
            e.preventDefault();
        });

        $('#navi-select-all-d-replace-btn').click(function(e) {
            ATS.selectedApiCalls = [];
            $.each($('.api-call-sel-ALL:visible'), function(key, value) {
                ATS.addToSelected($(value));
            });
            e.preventDefault();
        });

        $('#navi-deselect-all-d-btn').click(function(e) {
            $.each($('.api-call-sel-ALL:visible'), function(key, value) {
                ATS.removeFromSelected($(value));
            });
            e.preventDefault();
        });

        $('#navi-deselect-all-btn').click(function(e) {
            ATS.selectedApiCalls = [];
            $('.api-call-sel-ALL').prop('checked', false);
            ATS.selectedApiCallsChange();
            e.preventDefault();
        });

    }

    ATS.performSearch = function(searchStr) {
        if (searchStr == '') {
            $('.api-call-All').show();
        } else {
            $('.api-call-All').hide();
            $('.topic-link').css('font-weight', 'normal');
            for(var i=0; i<ATS.apiCalls.length; i++) {
                var apiCall = ATS.apiCalls[i];
                if (new RegExp(searchStr.toLowerCase()).test(apiCall.toLowerCase())) {
                    $('#api-call-' + apiCall).show();
                }
            }
        }
    }

    ATS.submitForm = function(form) {
        var url = '/nxt';
        var params = {};
        for (i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type != 'button' && form.elements[i].value && form.elements[i].value != 'submit') {
                params[form.elements[i].name] = form.elements[i].value;
            }
        }
        $.ajax({
            url: url,
            type: 'POST',
            data: params
        })
        .done(function(result) {
            var resultStr = JSON.stringify(JSON.parse(result), null, 4);
            var code_elem = form.getElementsByClassName("result")[0];
            code_elem.textContent = resultStr;
            hljs.highlightBlock(code_elem);
        })
        .error(function() {
            alert('API not available, check if Nxt Server is running!');
        });
        if ($(form).has('.uri-link').length > 0) { 
            var uri = '/nxt?' + jQuery.param(params);
            var html = '<a href="' + uri + '" target="_blank" style="font-size:12px;font-weight:normal;">Open GET URL</a>';
            form.getElementsByClassName("uri-link")[0].innerHTML = html;
        }
        return false;
    }

    ATS.selectedApiCallsChange = function() {
        var newUrl = '/test?requestTypes=' + encodeURIComponent(ATS.selectedApiCalls.join('_'));
        $('#navi-selected').attr('href', newUrl);
        $('#navi-selected').text('SELECTED (' + ATS.selectedApiCalls.length + ')');
        ATS.setCookie('selected_api_calls', ATS.selectedApiCalls.join('_'), 30);
        console.log(ATS.selectedApiCalls);
    }

    ATS.setSelectedApiCalls = function() {
        var calls = [];
        var getParam = ATS.getUrlParameter('requestTypes');
        var cookieParam = ATS.getCookie('selected_api_calls');
        if (getParam != undefined && getParam != '') {
            calls=getParam.split('_');
        } else if (cookieParam != undefined && cookieParam != ''){
            calls=cookieParam.split('_');
        }
        for (var i=0; i<calls.length; i++) {
            $('#api-call-sel-' + calls[i]).prop('checked', true);
        }
        return calls;
    } 
    
    ATS.addToSelected = function(elem) {
        var type=elem.attr('id').substr(13);
        elem.prop('checked', true);
        if($.inArray(type, ATS.selectedApiCalls) == -1) {
            ATS.selectedApiCalls.push(type);
            ATS.selectedApiCallsChange();
        }
    }
    
    ATS.removeFromSelected = function(elem) {
        var type=elem.attr('id').substr(13);
        elem.prop('checked', false);
        var index = ATS.selectedApiCalls.indexOf(type);
        if (index > -1) {
            ATS.selectedApiCalls.splice(index, 1);
            ATS.selectedApiCallsChange();
        }
    }

    return ATS;
}(ATS || {}, jQuery));

$(document).ready(function() {
    ATS.init();
});