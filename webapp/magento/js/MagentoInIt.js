jQuery(function() {
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
    jQuery('body').find('form div.form-group .required:input').each(function() {
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

    jQuery(".chosen-select").chosen();
});