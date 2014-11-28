jQuery(function() {
    jQuery.validator.setDefaults({
        // Note: Here we are allowing chosen-select to run validation as by default select or input having either of this classes marked as hidden.
        ignore: ':hidden:not(.chosen-select)'
    });

    jQuery.validator.addMethod('req', function(v, e, p) {
        switch(e.nodeName.toLowerCase()) {
            case 'select':
                var val = jQuery(e).val();
                return val && val.length > 0;
            case 'input':
                if(this.checkable(e)) {
                    return this.getLength(v, e) > 0;
                }
            default:
                return jQuery.trim(v).length > 0;
        }
    },
    function(v, e) {
        var label_text = getValidateElementLabel(v, e);
        return 'Please enter ' + label_text;
    }
);
    jQuery.validator.addMethod('phone', function(v, e, p) {
        return /^(\d{0,3}[ .-]?\d{3}[ .-]?\d{3}[ .-]?\d{4})?$/.test(v); 
        },
        'Please enter a valid phone number. Example: 123-123-123-1234.'
    );

    function getValidateElementLabel(v, e) {
        jQuery(e).siblings('label[class="error"]').remove();
        var label_text = jQuery(e).data('label') || jQuery(e).siblings('label:first').text();
        // can't use the title attribute here, as the validation API uses it to override the whole message
        if (label_text === undefined || label_text === "") {
            label_text = jQuery(e).attr('name');
        }
        return label_text;
    }

    jQuery.validator.addClassRules({
        'required': {
            req: true
        },
        'validate-phone': {
            phone: true
        }
    });

    initValidations = (function() {
        function setValidation(elt) {
            jQuery(elt).validate({
                errorPlacement: function(error, input_elt) {
                    var input_id = jQuery(input_elt).attr('id') || '',
                        msg_elt = '';
                    // This code is to support error placement in modal window, on page modal window rendering need to be fixed.
                    if(input_id !== undefined && input_id !== '') {
                        var modal_elt = input_elt.closest('.modal');
                        if(modal_elt.size() === 0) {
                            msg_elt = jQuery('#validate-' + input_id);
                        } else {
                            msg_elt = modal_elt.find('#validate-' + input_id);
                        }
                    }
                    // The element where validation message will be placed is identified by css selector #validate-<input field's name>
                    // If no such element exists, then the validation message will be appended to input field's parent element
                    if (jQuery(msg_elt).size() === 0) {
                        error.appendTo(input_elt.parent());
                    } else {
                        error.appendTo(msg_elt);
                    }
                }
            });
        }
        // Set validations on all the forms which have class requireValidation
        jQuery('form.requireValidation').each(function(i, elt) {
            setValidation(elt);
        });

        return function(elt) {
            jQuery(elt).find('form.requireValidation').each(function(i, elt) {
                setValidation(elt);
            });
        }
    }());
    jQuery('body').on('keyup change', '.chosen-select', function(){
        var elt = jQuery(this);
        if (elt.attr('value') === '') {
            elt.siblings('label.error').show();
        } else {
            elt.siblings('label.error').hide();
        }
    });

    function toggleDisplay(target, state, effect) {
        if (jQuery(target) !== undefined) {
            if (state === true) {
                if (effect === 'slide') {
                    jQuery(target).slideDown().removeClass('hide');
                } else if (effect === 'fade') {
                    jQuery(target).fadeIn().removeClass('hide');
                } else {
                    jQuery(target).show().removeClass('hide');
                }
            } else {
                if (effect === 'slide') {
                    jQuery(target).slideUp();
                } else if (effect === 'fade') {
                    jQuery(target).fadeOut();
                } else {
                    jQuery(target).hide();
                }
            }
        }
    }
    jQuery('body').on('change click', '[data-toggle-display]', function(e) {
        var effect = jQuery(this).data('toggle-effect');
        if (jQuery(this).is(':checkbox, :radio:checked')) {
            toggleDisplay(jQuery(this).data('toggle-display'), jQuery(this).is(':checked'), effect);
        } else {
            e.preventDefault();
            jQuery(jQuery(this).data('toggle-display')).each(function() {
                toggleDisplay(jQuery(this), !jQuery(this).is(':visible'), effect);
            });
        }
    });

    jQuery('body').on('change click', '[data-toggle-hide]', function(e) {
        var effect = jQuery(this).data('toggle-effect');
        if (jQuery(this).is(':checkbox, :radio:checked')) {
            toggleDisplay(jQuery(this).data('toggle-hide'), !jQuery(this).is(':checked'), effect);
        } else {
            e.preventDefault();
            jQuery(jQuery(this).data('toggle-hide')).each(function() {
                toggleDisplay(jQuery(this), !jQuery(this).is(':visible'), effect);
            });
        }
    });

    jQuery('body').on('click', 'a[data-ajax-update], button[data-ajax-update]', function(e) {
        e.preventDefault();
        jQuery.proxy(ajaxUpdater, {elt: this})();
    });
    rebindContainer();
});
    function ajaxUpdater() {
        var options = this,
            elt = jQuery(options.elt),
            callback = options.callback ? options.callback : jQuery.noop;
            beforeSendCallback = options.beforeSendCallback ? options.beforeSendCallback : jQuery.noop;
            url = elt.data('update-url'),
            to_update = jQuery(elt.data('ajax-update')),
            valid = true,
            param_source = jQuery(elt.data('param-source')),
            form_fields = jQuery.unique(jQuery.merge(param_source.find(':input').andSelf(), elt.find(':input').andSelf()).filter(':input')),
            data = form_fields.serializeArray();
            form_fields.filter(':visible').each(function() {
                var validator = jQuery(this).form().data('validator');
                if(validator !== undefined) {
                valid = valid && (validator.check(this) == false ? false : true);
                }
            });
        if (valid) {
            jQuery.ajax({
                async: true,
                type: 'post',
                url: url,
                data: data,
                beforeSend: function(xhr, settings) {
                    beforeSendCallback({
                        xhr: xhr,
                    });
                },
                complete: function(xhr, status) {
                    handleAjaxResponse({
                        xhr: xhr,
                        response: status,
                        display_success_method: to_update,
                        display_error_method: to_update,
                        status: status
                    });
                    callback(xhr.responseText, xhr);
                }
            });
        }
     }

    function handleAjaxResponse(options) {
        var response = options.response,
            xhr = options.xhr,
            data = jQuery(xhr.responseText).not('script').not('.messages'),
            scripts = jQuery(xhr.responseText).filter('script'),
            notification_messages = jQuery(xhr.responseText).filter('.messages').children(),
            to_update_selector = (response === 'success') ? options.display_success_method : options.display_error_method,
            to_update = jQuery(to_update_selector),
            default_dialog_title = (response === 'success') ? 'Notification' : 'Error',
            anim_method = (response === 'success') ? options.anim_method : '',
            anim_direction = (response === 'success' && options.anim_direction) ? options.anim_direction : '',
            new_dialog_title = (options.display_dialog_title) ? options.display_dialog_title : jQuery(window.modal).find('.modal-title'),
            new_dialog_update = options.new_dialog_update,
            data_dialog_width = (window.data_dialog_width) ? window.data_dialog_width : "default";
            if(jQuery(to_update_selector).find('.js-thfloat-foot').size() > 0){
                jQuery('.thfloat-table').remove();
            }
            if(new_dialog_title === undefined) {
                new_dialog_title = default_dialog_title;
            }
        // Check if we are supposed to redirect the user to a new page
        if (xhr.getResponseHeader('requestAction')) {
            window.location = xhr.getResponseHeader('requestAction');
            return;
        }
        if (data.size() > 0) {
            var focus_elt = jQuery(':focus'),
            focus_elt_id = focus_elt.attr('id');

            if (to_update.size() === 1 && new_dialog_update === undefined) {
                // We have exactly one element on the page to drop the data received
                if (anim_method === undefined) {
                    if (data.filter(to_update_selector).size() > 0) {
                        data = data.html();
                    }
                    jQuery(to_update).html(data);
                }
                // Close any open dialogs, so that user can see the results we just updated
                if (window.modal !== undefined) {
                    jQuery(window.modal).modal('hide');
                }
                rebindContainer(to_update);
                if (focus_elt_id !== undefined) {
                    var element = jQuery('#'+focus_elt_id);
                    element.focus().val(element.val());
                }
            }
            jQuery(scripts).appendTo('body');
        } else if (to_update.size() === 0 && window.modal !== undefined) {
            jQuery(window.modal).modal('hide');
        }
    }
    function rebindContainer(elt) {
        if (elt === undefined) {
            elt = jQuery('body');
        }
        jQuery(".chosen-select").chosen();
        initValidations(elt)
    jQuery(elt).find('form div.form-group .required:input').each(function() {
        if (!jQuery(this).closest('div.form-group').find('span.asterisk').size() > 0) {
            jQuery(this).closest('div.form-group > div').children('label').append('<span class="asterisk"> *</span>');
        }
    });
        jQuery('body').find('[data-dependent]').each(function() {
            var parent_elt = jQuery(this),
                child_elt = jQuery(parent_elt.data('dependent'));
            parent_elt.data('child-clone', child_elt.clone());
            jQuery('body').on('change', '[data-dependent]', function() {
                var parent_elt = jQuery(this),
                    child_elt = jQuery(parent_elt.data('dependent')),
                    child_clone = parent_elt.data('child-clone'),
                    selected_title = jQuery(parent_elt).find(':selected').attr('title');
                jQuery(child_elt).empty();
                child_clone.children().each(function() {
                    if (jQuery(this).attr('value') === '') {
                        jQuery(child_elt).append(jQuery(this).clone());
                    }
                    if (selected_title !== '' && (jQuery(this).attr('label') === selected_title || jQuery(this).hasClass(selected_title))) {
                        jQuery(child_elt).append(jQuery(this).clone());
                    }
                });
            });
            jQuery(this).change();
        });

    }
