/* 
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.data.validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator.Context;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.ConstraintDescriptor;

import com.vaadin.data.Validator;

/**
 * Vaadin {@link Validator} using the JSR-303 (javax.validation)
 * annotation-based bean validation.
 * 
 * The annotations of the fields of the beans are used to determine the
 * validation to perform.
 * 
 * Note that a JSR-303 implementation (e.g. Hibernate Validator or Apache Bean
 * Validation - formerly agimatec validation) must be present on the project
 * classpath when using bean validation.
 * 
 * @since 7.0
 * 
 * @author Petri Hakala
 * @author Henri Sara
 */
public class BeanValidationValidator implements Validator {

    private static final long serialVersionUID = 1L;
    private static ValidatorFactory factory;

    private transient javax.validation.Validator javaxBeanValidator;
    private String propertyName;
    private Class<?> beanClass;
    private Locale locale;

    /**
     * Simple implementation of a message interpolator context that returns
     * fixed values.
     */
    protected static class SimpleContext implements Context, Serializable {

        private final Object value;
        private final ConstraintDescriptor<?> descriptor;

        /**
         * Create a simple immutable message interpolator context.
         * 
         * @param value
         *            value being validated
         * @param descriptor
         *            ConstraintDescriptor corresponding to the constraint being
         *            validated
         */
        public SimpleContext(Object value, ConstraintDescriptor<?> descriptor) {
            this.value = value;
            this.descriptor = descriptor;
        }

        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return descriptor;
        }

        public Object getValidatedValue() {
            return value;
        }

    }

    /**
     * Creates a Vaadin {@link Validator} utilizing JSR-303 bean validation.
     * 
     * @param beanClass
     *            bean class based on which the validation should be performed
     * @param propertyName
     *            property to validate
     */
    public BeanValidationValidator(Class<?> beanClass, String propertyName) {
        this.beanClass = beanClass;
        this.propertyName = propertyName;
        locale = Locale.getDefault();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.data.Validator#validate(java.lang.Object)
     */
    public void validate(final Object value) throws InvalidValueException {
        Set<?> violations = getJavaxBeanValidator().validateValue(beanClass,
                propertyName, value);
        if (violations.size() > 0) {
            List<String> exceptions = new ArrayList<String>();
            for (Object v : violations) {
                final ConstraintViolation<?> violation = (ConstraintViolation<?>) v;
                String msg = getJavaxBeanValidatorFactory()
                        .getMessageInterpolator().interpolate(
                                violation.getMessageTemplate(),
                                new SimpleContext(value, violation
                                        .getConstraintDescriptor()), locale);
                exceptions.add(msg);
            }
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < exceptions.size(); i++) {
                if (i != 0) {
                    b.append("<br/>");
                }
                b.append(exceptions.get(i));
            }
            throw new InvalidValueException(b.toString());
        }
    }

    /**
     * Sets the locale used for validation error messages.
     * 
     * Revalidation is not automatically triggered by setting the locale.
     * 
     * @param locale
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Gets the locale used for validation error messages.
     * 
     * @return locale used for validation
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the underlying JSR-303 bean validator factory used. A factory is
     * created using {@link Validation} if necessary.
     * 
     * @return {@link ValidatorFactory} to use
     */
    protected static ValidatorFactory getJavaxBeanValidatorFactory() {
        if (factory == null) {
            factory = Validation.buildDefaultValidatorFactory();
        }

        return factory;
    }

    /**
     * Returns a shared Validator instance to use. An instance is created using
     * the validator factory if necessary and thereafter reused by the
     * {@link BeanValidationValidator} instance.
     * 
     * @return the JSR-303 {@link javax.validation.Validator} to use
     */
    protected javax.validation.Validator getJavaxBeanValidator() {
        if (javaxBeanValidator == null) {
            javaxBeanValidator = getJavaxBeanValidatorFactory().getValidator();
        }

        return javaxBeanValidator;
    }

}