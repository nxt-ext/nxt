/*
    jQuery Masked Input Plugin
    Copyright (c) 2007 - 2015 Josh Bush (digitalbush.com)
    Licensed under the MIT license (http://digitalbush.com/projects/masked-input-plugin/#license)
    Version: 1.4.1
*/
!function(factory) {
    "function" == typeof define && define.amd ? define([ "jquery" ], factory) : factory("object" == typeof exports ? require("jquery") : jQuery);
}(function($) {
    var caretTimeoutId, ua = navigator.userAgent, iPhone = /iphone/i.test(ua), chrome = /chrome/i.test(ua), android = /android/i.test(ua);
    $.mask = {
        definitions: {
            "9": "[0-9]",
            a: "[A-Za-z]",
            "*": "[A-Za-z0-9]"
        },
        autoclear: !0,
        dataName: "rawMaskFn",
        placeholder: "_"
    }, $.fn.extend({
        caret: function(begin, end) {
            var range;
            if (0 !== this.length && !this.is(":hidden")) return "number" == typeof begin ? (end = "number" == typeof end ? end : begin, 
            this.each(function() {
                this.setSelectionRange ? this.setSelectionRange(begin, end) : this.createTextRange && (range = this.createTextRange(), 
                range.collapse(!0), range.moveEnd("character", end), range.moveStart("character", begin), 
                range.select());
            })) : (this[0].setSelectionRange ? (begin = this[0].selectionStart, end = this[0].selectionEnd) : document.selection && document.selection.createRange && (range = document.selection.createRange(), 
            begin = 0 - range.duplicate().moveStart("character", -1e5), end = begin + range.text.length), 
            {
                begin: begin,
                end: end
            });
        },
        unmask: function() {
            return this.trigger("unmask");
        },
        mask: function(mask, settings) {
            var input, defs, tests, partialPosition, firstNonMaskPos, lastRequiredNonMaskPos, len, oldVal;
            if (!mask && this.length > 0) {
                input = $(this[0]);
                var fn = input.data($.mask.dataName);
                return fn ? fn() : void 0;
            }
            return settings = $.extend({
                autoclear: $.mask.autoclear,
                placeholder: $.mask.placeholder,
                completed: null
            }, settings), defs = $.mask.definitions, tests = [], partialPosition = len = mask.length, 
            firstNonMaskPos = null, $.each(mask.split(""), function(i, c) {
                "?" == c ? (len--, partialPosition = i) : defs[c] ? (tests.push(new RegExp(defs[c])), 
                null === firstNonMaskPos && (firstNonMaskPos = tests.length - 1), partialPosition > i && (lastRequiredNonMaskPos = tests.length - 1)) : tests.push(null);
            }), this.trigger("unmask").each(function() {
                function tryFireCompleted() {
                    if (settings.completed) {
                        for (var i = firstNonMaskPos; lastRequiredNonMaskPos >= i; i++) if (tests[i] && buffer[i] === getPlaceholder(i)) return;
                        settings.completed.call(input);
                    }
                }
                function getPlaceholder(i) {
                    return settings.placeholder.charAt(i < settings.placeholder.length ? i : 0);
                }
                function seekNext(pos) {
                    for (;++pos < len && !tests[pos]; ) ;
                    return pos;
                }
                function seekPrev(pos) {
                    for (;--pos >= 0 && !tests[pos]; ) ;
                    return pos;
                }
                function shiftL(begin, end) {
                    var i, j;
                    if (!(0 > begin)) {
                        for (i = begin, j = seekNext(end); len > i; i++) if (tests[i]) {
                            if (!(len > j && tests[i].test(buffer[j]))) break;
                            buffer[i] = buffer[j], buffer[j] = getPlaceholder(j), j = seekNext(j);
                        }
                        writeBuffer(), input.caret(Math.max(firstNonMaskPos, begin));
                    }
                }
                function shiftR(pos) {
                    var i, c, j, t;
                    for (i = pos, c = getPlaceholder(pos); len > i; i++) if (tests[i]) {
                        if (j = seekNext(i), t = buffer[i], buffer[i] = c, !(len > j && tests[j].test(t))) break;
                        c = t;
                    }
                }
                function androidInputEvent() {
                    var curVal = input.val(), pos = input.caret();

                      console.log('oldVal: ' + oldVal);
                      console.log('oldval.length: ' + oldVal.length);
                      console.log('curVal: ' + curVal);
                      console.log('curval.length: ' + curVal.length);

                    if (oldVal && oldVal.length && oldVal.length > curVal.length) {
                        console.log(input.caret());
                        for (checkVal(!0); pos.begin > 0 && !tests[pos.begin - 1]; ) pos.begin--;
                        if (0 === pos.begin) for (;pos.begin < firstNonMaskPos && !tests[pos.begin]; ) pos.begin++;
                        input.caret(pos.begin, pos.begin);
                        console.log(input.caret());
                    } else {
                        console.log(input.caret());
                        console.log('checkval: ' + checkVal(!0));
                        console.log('posbegin: ' + pos.begin);
                        console.log('len: ' + len);

                          console.log('---- start for loop ----');
                        for (checkVal(!0); pos.begin < len && !tests[pos.begin]; ) {
                          console.log('pos.begin:' + pos.begin);
                          console.log('tests-pos.begin: ' + !tests[pos.begin]);
                          pos.begin++;
                        }
                          console.log('---- end for loop ----');

                          console.log(input.caret());
                          console.log('pos.begin:' + pos.begin);
                        input.caret(pos.begin, pos.begin);
                          console.log(input.caret());
                    }
                    console.log(input.caret());
                    tryFireCompleted();
                }//1.4 and not in wesley's version 1.3
                function blurEvent() {
                    checkVal(), input.val() != focusText && input.change();
                }//1.4 and not in wesley's version 1.3


                function keydownEvent(e) {
                  console.log('keycode pressed: ' + e.which);
                  console.log(input.caret());

                    if (!input.prop("readonly")) {

                        //ignore tab -- taken from 1.3.1 wesley - START -dux
                        if (e.keyCode == 9) {
                          return true;
                        }
                        if (e.keyCode == 8) {
                          var currentInput = input.val();
                          var pos = input.caret();

                          console.log(currentInput + pos);


                          if (settings.unmask !== false) {
                            //backspace, remove
                            if ((pos.begin == 0 && pos.end == 24) || (currentInput == "NXT-____-____-____-_____" && pos.begin == 4)) {
                              input.val("");
                              $(this).trigger("unmask");
                              return;
                            }
                          }
                        }// -- taken from 1.3.1 wesley - END -dux


                        var pos, begin, end, k = e.which || e.keyCode;

                        oldVal = input.val(), 8 === k || 46 === k || iPhone && 127 === k ? (pos = input.caret(), 
                        begin = pos.begin, end = pos.end, end - begin === 0 && (begin = 46 !== k ? seekPrev(begin) : end = seekNext(begin - 1), 
                        end = 46 === k ? seekNext(end) : end), clearBuffer(begin, end), shiftL(begin, end - 1), 
                        e.preventDefault()) : 13 === k ? blurEvent.call(this, e) : 27 === k && (input.val(focusText), 
                        input.caret(0, checkVal()), e.preventDefault());
                    }
                }


                function keypressEvent(e) {
                    if (!input.prop("readonly")) {


                        //ignore tab -- taken from 1.3.1 wesley - START -dux
                        if (e.keyCode == 9) {
                          return true;
                        }// -- taken from 1.3.1 wesley - END -dux


                        var p, c, next, k = e.which || e.keyCode, pos = input.caret();
                        if (!(e.ctrlKey || e.altKey || e.metaKey || 32 > k) && k && 13 !== k) {
                            if (pos.end - pos.begin !== 0 && (clearBuffer(pos.begin, pos.end), shiftL(pos.begin, pos.end - 1)), 
                            p = seekNext(pos.begin - 1), len > p && (c = String.fromCharCode(k), tests[p].test(c))) {
                                if (shiftR(p), buffer[p] = c, writeBuffer(), next = seekNext(p), android) {
                                    var proxy = function() {
                                        $.proxy($.fn.caret, input, next)();
                                    };
                                    setTimeout(proxy, 0);
                                } else input.caret(next);
                                pos.begin <= lastRequiredNonMaskPos && tryFireCompleted();
                            }
                            e.preventDefault();
                        }

                        //taken from 1.3.1 wesley - START -dux
                        if (/^NXT\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{5}/i.test(input.val())) {
                          input.trigger("checkRecipient");
                        }
                        //taken from 1.3.1 wesley - END -dux
                    }
                }
                function clearBuffer(start, end) {
                    var i;
                    for (i = start; end > i && len > i; i++) tests[i] && (buffer[i] = getPlaceholder(i));
                }
                function writeBuffer() {
                    input.val(buffer.join(""));
                }
                function checkVal(allow) {

                    //taken from 1.3.1 wesley - START -dux
                    input.val(input.val().replace(/^\s*NXT\-\s*NXT/i, "NXT-"));
                    //taken from 1.3.1 wesley - END -dux


                    var i, c, pos, test = input.val(), lastMatch = -1;
                    for (i = 0, pos = 0; len > i; i++) if (tests[i]) {
                        for (buffer[i] = getPlaceholder(i); pos++ < test.length; ) if (c = test.charAt(pos - 1), 
                        tests[i].test(c)) {
                            buffer[i] = c, lastMatch = i;
                            break;
                        }
                        if (pos > test.length) {
                            clearBuffer(i + 1, len);
                            break;
                        }
                    } else buffer[i] === test.charAt(pos) && pos++, partialPosition > i && (lastMatch = i);
                    return allow ? writeBuffer() : partialPosition > lastMatch + 1 ? settings.autoclear || buffer.join("") === defaultBuffer ? (input.val() && input.val(""), 
                    clearBuffer(0, len)) : writeBuffer() : (writeBuffer(), input.val(input.val().substring(0, lastMatch + 1))), 
                    partialPosition ? i : firstNonMaskPos;
                }
                var input = $(this), buffer = $.map(mask.split(""), function(c, i) {
                    return "?" != c ? defs[c] ? getPlaceholder(i) : c : void 0;
                }), defaultBuffer = buffer.join(""), focusText = input.val();


                //taken from 1.3.1 wesley - START -dux
                if (settings.noMask) {
                  input.bind("keyup.remask", function(e) {
                    if (input.val().toLowerCase() == "nxt-") {
                      input.val("").mask("NXT-****-****-****-*****").unbind(".remask").trigger("focus");
                    }
                  }).bind("paste.remask", function(e) {
                    setTimeout(function() {
                      var newInput = input.val();

                      if (/^NXT\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{5}/i.test(newInput) || /^NXT[A-Z0-9]{17}/i.test(newInput)) {
                        input.mask("NXT-****-****-****-*****").trigger("checkRecipient").unbind(".remask");
                      }
                    }, 0);
                  });
                  return;
                }

                input.addClass("masked");
                //taken from 1.3.1 wesley - END -dux




                input.data($.mask.dataName, function() {
                    return $.map(buffer, function(c, i) {
                        return tests[i] && c != getPlaceholder(i) ? c : null;
                    }).join("");
                }), input.one("unmask", function() {

                    input.off(".mask").removeData($.mask.dataName);

                    //taken from 1.3.1 wesley - START -dux
                    //if (!removeCompletely) { COMMENTED OUT FROM WESLEY'S 1.3 as 1.4 doesn't have removeCompletely in this call
                      input.bind("keyup.remask", function(e) {
                        if (input.val().toLowerCase() == "nxt-") {
                          input.val("").mask("NXT-****-****-****-*****").unbind(".remask").trigger("focus");
                        }
                      }).bind("paste.remask", function(e) {
                        setTimeout(function() {
                          var newInput = input.val();

                          if (/^NXT\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{5}/i.test(newInput) || /^NXT[A-Z0-9]{17}/i.test(newInput)) {
                            input.mask("NXT-****-****-****-*****").trigger("checkRecipient").unbind(".remask");
                          }
                        }, 0);
                      });
                    //}
                    //taken from 1.3.1 wesley - END -dux


                }).on("focus.mask", function() {
                    if (!input.prop("readonly")) {
                        clearTimeout(caretTimeoutId);
                        var pos;
                        focusText = input.val(), pos = checkVal(), caretTimeoutId = setTimeout(function() {
                            input.get(0) === document.activeElement && (writeBuffer(), pos == mask.replace("?", "").length ? input.caret(0, pos) : input.caret(pos));
                        }, 10);
                    }
                }).on("blur.mask", blurEvent).on("keydown.mask", keydownEvent).on("keypress.mask", keypressEvent).on("input.mask paste.mask", function() {
                    var oldInput = input.val();

                    input.prop("readonly") || setTimeout(function() {

                        //taken from 1.3.1 wesley - START -dux
                        var newInput = input.val();

                        var pasted = text_diff(oldInput, newInput);

                        if (/^NXT\-[0-9]{19,20}$/i.test(pasted)) {
                          //old style accounts..
                          input.val("").trigger("oldRecipientPaste");
                        } else {
                          var match = /^NXT\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{5}/i.exec(pasted);

                          if (match && match[0]) {
                            input.val(match[0]).trigger("checkRecipient");
                          } else {
                            match = /^NXT[A-Z0-9]{17}/i.exec(pasted);
                            if (match && match[0]) {
                              input.val(pasted).trigger("checkRecipient");
                            } else {
                              input.trigger("checkRecipient");
                            }
                          }
                        }
                        //taken from 1.3.1 wesley - END -dux

                        var pos = checkVal(!0);
                        console.log("checkval" + pos);
                        input.caret(pos), tryFireCompleted();
                    }, 0);
                }), chrome && android && input.off("input.mask").on("input.mask", androidInputEvent), 
                checkVal();
            });
        }
    });


    //taken from 1.3.1 wesley - START -dux
    function text_diff(first, second) {
      first = first.toUpperCase();
      second = second.toUpperCase();

      var start = 0;
      while (start < first.length && first[start] == second[start]) {
        ++start;
      }
      var end = 0;
      while (first.length - end > start && first[first.length - end - 1] == second[second.length - end - 1]) {
        ++end;
      }
      end = second.length - end;

      var diff = second.substr(start, end - start);

      if (/^NXT\-/i.test(second) && !/^NXT\-/i.test(diff)) {
        diff = "NXT-" + diff;
      }

      return diff;
    }
    //taken from 1.3.1 wesley - END -dux

});
