package com.excelsecu.androidx2j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;

import com.excelsecu.androidx2j.AX2JCodeBlock.AX2JCode;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AX2JClassTranslator {
    public static HashMap<String, Class<?>> typeMap = new HashMap<String, Class<?>>() {
        private static final long serialVersionUID = -4934808097054114253L;

        {
            put("int", Integer.class);
            put("float", Float.class);
            put("boolean", Boolean.class);
            put("long", Long.class);
            put("PorterDuff.Mode", PorterDuff.Mode.class);
            put("TextView.BufferType", TextView.BufferType.class);
            put("TextUtils.TruncateAt", TextUtils.TruncateAt.class);
            put("ImageView.ScaleType", TextUtils.TruncateAt.class);
            
            put(Integer.class);
            put(Float.class);
            put(Boolean.class);
            put(Long.class);
            put(PorterDuff.Mode.class);
            put(TextView.BufferType.class);
            put(TextUtils.TruncateAt.class);
            put(ImageView.ScaleType.class);
            
            put(String.class);
            put(Drawable.class);
            put(ColorStateList.class);
            put(Transition.class);
            put(CharSequence.class);
            put(KeyListener.class);
            put(Paint.class);
            put(LayoutTransition.class);
            put(Context.class);
            put(Typeface.class);
            put(InputFilter.class);
            put(TransformationMethod.class);
        }
        
        private void put(Class<?> type) {
            put(type.getSimpleName(), type);
        }
        
    };
    
    private Class<?> type;
    private List<AX2JAttribute> attributeList = new ArrayList<AX2JAttribute>();
    private List<AX2JMethod> methodList = new ArrayList<AX2JMethod>();
    
    public AX2JClassTranslator(Class<?> type) {
        this.type = type;
        attributeList = new ArrayList<AX2JAttribute>();
    }
    
    public void add(String qNameString, String methodString) {
        add(string2QName(qNameString), methodString, 0);
    }
    
    public void add(String qNameString, String methodString, int methodType) {
        add(string2QName(qNameString), methodString, methodType);
    }
    
    public void add(QName name, String methodString, int methodType) {
        AX2JMethod method = new AX2JMethod(name, methodString);
        addAttribute(name, method, methodType);
    }
    
    public boolean remove(String qNameString, String methodString) {
        QName name = string2QName(qNameString);
        AX2JAttribute attribute = findAttribute(name);
        if (attribute != null) {
            AX2JMethod method = new AX2JMethod(name, methodString);
            return attribute.removeMethod(method);
        }
        
        return false;
    }
    
    public AX2JAttribute get(QName name) {
        for (AX2JAttribute attribute : attributeList) {
            if (attribute.getName().equals(name)) {
                return attribute.clone();
            }
        }
        return null;
    }
    
    public AX2JAttribute findAttribute(QName name) {
        for (AX2JAttribute attribute : attributeList) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }
    
    
    public AX2JMethod findMethod(AX2JMethod oldMethod) {
        for (AX2JMethod method : methodList) {
            if (method.equals(oldMethod)) {
                return method;
            }
        }
        return null;
    }
    
    public void addAttribute(QName name, AX2JMethod method, int methodType) {
    	
        AX2JAttribute attribute = findAttribute(name);
        if (attribute == null) {
            attribute = new AX2JAttribute(name, methodType, type);
            attributeList.add(attribute);
        } else {
            attribute.setMethodType(methodType);
        }
        
        AX2JMethod oldMethod = findMethod(method);
        if (oldMethod != null) {
            method = oldMethod;
        } else {
            methodList.add(method);
        }
        
        if (method.findAttribute(attribute) == null) {
        	method.addRelativeAttribute(attribute);
        }
        if (attribute.findMethod(method) == null) {
            attribute.addRelativeMethod(method); 
        }
    }
    
    public static QName string2QName(String qNameString) {
        QName name = null;
        if (qNameString.indexOf(':') != -1) {
            String prefixString = qNameString.substring(0, qNameString.indexOf(':'));
            String nameString = qNameString.substring(qNameString.indexOf(':') + 1);
            if (prefixString.equals("android")) {
                name = new QName(nameString, Config.ANDROID_NAMESPACE);
            } else {		//only support android name space
                name = new QName(nameString);
            }
        } else {
            name = new QName(qNameString);
        }
        return name;
    }
    
    public static Class<?> getType(String typeString) {
        Class<?> type = typeMap.get(typeString);
        return type;
    }
    
    public void translate(AX2JCodeBlock codeBlock, Attribute attr) {
    	translate(codeBlock, attr, 0);
    }
    
    public void translate(AX2JCodeBlock codeBlock, Attribute attr, int priority) {
    	
        AX2JAttribute attribute = findAttribute(attr.getQName());
        if (attribute == null) {
            throw new AX2JException(AX2JException.ATTRIBUTE_NOT_FOUND, attr.asXML());
        }
        attribute.setValue(attr);
        
        AX2JMethod method = chooseMethod(attribute);
        if (method == null || method.getName().equals("")) {
            throw new AX2JException(AX2JException.METHOD_NOT_FOUND, attr.asXML());
        }
        
        String value = translateValue(codeBlock, attribute, method);
        
        codeBlock.add(method, value, attribute.getType() + (priority << AX2JAttribute.TYPE_PRIORITY_INDEX));
    }
    
    /**
     * Translate a XML attribute's value to a Java method's value.
     * @param attr the attribute to be translated
     * @return the value after translating
     */
    private String translateValue(AX2JCodeBlock codeBlock, AX2JAttribute attribute, AX2JMethod method) {
        int argOrder = attribute.getTypeValue(AX2JAttribute.TYPE_ARGUMENTS_ORDER);
        if (argOrder == AX2JAttribute.TYPE_ARGUMENTS_ALL_THE_SAME) {
        	argOrder = 1;
        }
        Class<?> argType = method.getArgType(argOrder);
        
        return translateValue(codeBlock, attribute.getValue(), argType);
    }
    
    protected final String translateValue(AX2JCodeBlock codeBlock, Attribute attribute, Class<?> argType) {
        String value = attribute.getValue();
        String name = attribute.getQualifiedName();
        
        if (argType.equals(Integer.class)) {
            //dp, px, sp
            if (value.matches("[0-9.]+dp")) {
                value = value.substring(0, value.length() - 2);
                value = "(int) (" + value + " * scale + 0.5f)";
                codeBlock.add("final float scale = context.getResources().getDisplayMetrics().density;\n", AX2JCode.PRIORITY_FIRST);
            } else if (value.matches("[0-9.]+sp")) {
                value = value.substring(0, value.length() - 2);
            } else if (value.matches("[0-9]+px")) {
                value = value.substring(0, value.length() - 2);
            } else if (value.equals("fill_parent") || value.equals("match_parent")) {
                value = "ViewGroup.LayoutParams.MATCH_PARENT";
                codeBlock.addImport(ViewGroup.class.getName());
            } else if (value.equals("wrap_content")) {
                value = "ViewGroup.LayoutParams.WRAP_CONTENT";
                codeBlock.addImport(ViewGroup.class.getName());
            }
            
            //id
            else if (value.startsWith("@+id/") || value.startsWith("@id/")) {
                value = value.substring(value.indexOf('/') + 1);
                value = Config.R_CLASS + ".id." + value;
                codeBlock.addImport(Config.PACKAGE_NAME + "." + Config.R_CLASS);
            }
            
            //string
            else if (value.contains("@string/")) {
                value = value.substring(value.indexOf('/') + 1);
                value = Config.R_CLASS + ".string." + value;
                value = Config.RESOURCES_NAME + ".getString(" + value + ")";
            } else if (name.equals("android:text") ||
                    name.equals("android:hint")) {
                value = "\"" + value + "\"";
            }
            
            //color
            else if (value.matches("#[0-9a-fA-F]+")) {
                if (value.length() == 4) {
                    value = "#" + value.charAt(1) + '0' + value.charAt(2) + '0' +
                            value.charAt(3) + '0';
                } else if (value.length() == 5) {
                    value = "#" + value.charAt(1) + '0' + value.charAt(2) + '0' +
                            value.charAt(3) + '0' + value.charAt(4) + '0';
                }
                value = "Color.parseColor(\"" + value + "\")";
                codeBlock.addImport(Color.class.getName());
            } else if (value.matches("@android:color/.+")) {
                value = value.substring(value.indexOf('/') + 1);
                value = value.toUpperCase();
                value = "Color." + value;
                codeBlock.addImport(Color.class.getName());
            } else if (value.matches("@color/.+")) {
                value = value.substring(value.indexOf('/') + 1);
                value = Config.R_CLASS + ".color." + value;
                value = Config.RESOURCES_NAME + ".getColor(" + value + ")";
            }
            
            //visibility
            else if (value.equals("gone") || value.equals("visibile") ||
                    value.equals("invisibile")) {
                value = "View." + value.toUpperCase();
                codeBlock.addImport(View.class.getName());
            }
            
            //orientation
            else if (value.equals("vertical")) {
                value = "LinearLayout.VERTICAL";
            } else if (value.equals("horizontal")) {
                value = "LinearLayout.HORIZONTAL";
            }
            
            //gravity
            else if (name.equals("android:gravity") ||
                    name.equals("android:layout_gravity")) {
                value = Utils.prefixParams(value, "Gravity");
                codeBlock.addImport(Gravity.class.getName());
            }
            
            //margin
            else if (name.matches("android:layout_margin(Left)|(Top)|(Right)|(Bottom)")) {
                codeBlock.addImport(ViewGroup.class.getName());
            }
            
            //text
            else if (name.equals("android:textAppearance")) {
            	String style = AX2JStyle.getStyle(value).name;
            	style = style.replace('.', '_');
            	style = "android.R.style." + style;
            	value = style;
            }
            
            /** independent part **/
            //RelativeLayout rule
            if (Utils.findRule(name) != null) {
            	if (value.equals("true")) {
                	value = "RelativeLayout.TRUE";
            	} else if (value.equals("false")) {
                	value = "RelativeLayout.FALSE";
            	}
                codeBlock.addImport(RelativeLayout.class.getName());
            }
            
            //divider
            if (name.equals("android:divider")) {
                codeBlock.addImport(ColorDrawable.class.getName());
            }
            
            //id
            if (value.startsWith("@drawable/") ||
                    value.startsWith("@color/") ||
                    value.startsWith("@string/")) {
                codeBlock.addImport(Config.PACKAGE_NAME + "." + Config.RESOURCES_CLASS);
                codeBlock.addImport(Config.PACKAGE_NAME + "." + Config.R_CLASS);
                codeBlock.add(Config.RESOURCES_CLASS + " " + Config.RESOURCES_NAME + 
                        " = new " + Config.RESOURCES_CLASS + "(context);\n",
                        AX2JCode.PRIORITY_FIRST);
            }
        }
        
        //CharSequence & String
        else if (argType.equals(CharSequence.class) || argType.equals(String.class)) {
            value = "\"" + value + "\"";
        }
        
        else if (argType.equals(Float.class)) {
            //float
            value = value + "f";
        }
        
        else if (argType.equals(Drawable.class) || argType.equals(ColorStateList.class)) {
            //drawable
            if (value.startsWith("@drawable/")) {
                value = value.substring(value.indexOf('/') + 1);
                value = Config.R_CLASS + ".drawable." + value;
                if (name.contains("Color") ||
                        name.contains("TintList")) {
                    value = "resources.getColorStateList(" + value + ")";
                } else {
                    value = "resources.getDrawable(" + value + ")";
                }
            }
        }
        
        else if (argType.equals(TransformationMethod.class)) {
            //text
            if (name.equals("android:password")) {
                value = "new PasswordTransformationMethod()";
                codeBlock.addImport(PasswordTransformationMethod.class.getName());
            } else if (name.equals("android:singleLine")) {
                value = "new SingleLineTransformationMethod()";
                codeBlock.addImport(SingleLineTransformationMethod.class.getName());
            } else if (name.equals("android:inputType")) {
                String error = value; 
                value = Config.INPUT_TYPE_MAP.get(value);
                if (value == null) {
                    throw new AX2JException(AX2JException.ATTRIBUTE_VALUE_ERROR, error);
                }
                value = Utils.prefixParams(value, "InputType");
                codeBlock.addImport(InputType.class.getName());
            } else if (name.equals("android:ellipsize")) {
                value = value.toUpperCase();
                value = "TextUtils.TruncateAt." + value;
                codeBlock.addImport(TextUtils.class.getName());
            }
        }
        
        else if (argType.equals(TextView.BufferType.class)) {
            value = "TextView.BufferType." + value.toUpperCase();
            codeBlock.addImport(TextView.class.getName());
        }
        
        return value;
    }
    
    /**
     * find the best method that suits the attribute value
     * @param attribute
     * @return the best method or the first relative method in relative list
     */
    private AX2JMethod chooseMethod(AX2JAttribute attribute) {
        String name = attribute.getName().getQualifiedName();
        String value = attribute.getValue().getValue();
        List<AX2JMethod> methodList = attribute.getRelativeMethodList();
        AX2JMethod bestMethod = methodList.get(0);
        
        if (name.equals("android:text")) {
            if (value.startsWith("@+id/") || value.startsWith("@id/")) {
                return attribute.findMethodByArgument(Integer.class);
            } else {
                return attribute.findMethodByArgument(CharSequence.class);
            }
        } else if (name.startsWith("android:padding") &&!name.equals("android:padding")) {
            Element element = attribute.getValue().getParent();
            if (element.attributeValue("android:paddingStart") != null ||
                    element.attributeValue("android:paddingEnd") != null) {
                return attribute.findMethodByName("setPaddingRelative");
            } else {
                return attribute.findMethodByName("setPadding");
            }
        } else if (name.startsWith("android:drawable") && !name.equals("android:drawablePadding")) {
            Element element = attribute.getValue().getParent();
            if (element.attributeValue("android:drawableStart") != null ||
                    element.attributeValue("android:drawableEnd") != null) {
                return attribute.findMethodByName("setCompoundDrawablesRelativeWithIntrinsicBounds");
            } else {
                return attribute.findMethodByName("setCompoundDrawablesWithIntrinsicBounds");
            }
        }
        
        return bestMethod;
    }
    
    public Class<?> getType() {
        return type;
    }
    
    public List<AX2JAttribute> getAttributeList() {
        return attributeList;
    }
    
    public List<AX2JMethod> getMethodList() {
        return methodList;
    }
    
    public String toString() {
        StringBuffer content = new StringBuffer();
        for (AX2JAttribute attribute : attributeList) {
            content.append(attribute.toString());
        }
        return content.toString();
    }
}