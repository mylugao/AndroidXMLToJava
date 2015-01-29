package com.excelsecu.androidx2j;

import java.util.List;

import org.dom4j.Attribute;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

public class ShapeTranslater extends BaseTranslator {
    private static final String[] ORIENTATION = new String[] {"RIGHT_LEFT", "BR_TL",
                        "BOTTOM_TOP", "BL_TR", "LEFT_RIGHT", "TL_BR", "TOP_BOTTOM", "TR_BL"};
    
    public ShapeTranslater(AX2JNode root) {
        super(root);
    }
    
    @Override
    public String translate() {
        addImport(GradientDrawable.class.getName());
        String javaBlock = constructObject();
        getRoot().setObjectName(Utils.classToObject(GradientDrawable.class.getSimpleName()));
        javaBlock += super.translate();
        return javaBlock;
    }
    
    @Override
    public String translateValue(Attribute attr) {
        String attrName = attr.getQualifiedName();
        String value = attr.getValue();
        if (attrName.equals("android:type")) {
            value = "GradientDrawable." + value.toUpperCase() + "_GRADIENT";
        } else if (attrName.equals("android:shape")) {
            value = "GradientDrawable." + value.toUpperCase();
        } else if (attrName.equals("android:angle")) {
            int ordinal = Integer.parseInt(value);
            ordinal = ordinal / 45;
            if (ordinal < 0) {
                ordinal += 8;
            }
            value = "GradientDrawable.Orientation." + ORIENTATION[ordinal];
        }

        //nothing change
        if (value.equals(attr.getValue())) {
            return super.translateValue(attr);
        } else {
            return value;
        }
    }
    
    /**
     * Translate a Android XML Node to a Java method block.
     * @return the Java block
     */
    @Override
    protected String translateNode(AX2JNode node) {
        String javaBlock = "";
        SpecialTranslator specialTranslater = new SpecialTranslator(node);
        addImport(Context.class.getName());
        for (Attribute a : node.getAttributes()) {
            String attrMethod = translateAttribute(a, node, specialTranslater);
            if (!attrMethod.startsWith("//")) {
                extraHandle(node, a);
            }
            javaBlock += attrMethod;
        }
        javaBlock += "\n";
        
        return javaBlock;
    }
    
    private String translateAttribute(Attribute attr, AX2JNode node,
            SpecialTranslator specialTranslator) throws AX2JException {
        String attrMethod = "";
        try {
            attrMethod = super.translateAttribute(attr, node);
        } catch (AX2JException e) {
            try {
                //deal with the attributes that doesn't match the XML attributes table
                attrMethod = specialTranslator.translate(attr);
            } catch (AX2JException e1) {
                //translator can not translate this attribute
                attrMethod = "//" + attr.getQualifiedName() + "=\"" +
                        attr.getValue() + "\";\t//not support\n";
            }
        }
        return attrMethod;
    }
    
    private String constructObject() {
        String javaBlock = "";
        for (AX2JNode n : getRoot().getChildren()) {
            if (n.getLabelName().equals("gradient")) {
                    Attribute attrStartColor = n.findAttrByName("android:startColor");
                    Attribute attrCenterColor = n.findAttrByName("android:centerColor");
                    Attribute attrEndColor = n.findAttrByName("android:endColor");
                    Attribute attrOrientation = n.findAttrByName("android:angle");
                    String startColor = attrStartColor == null? "0" : translateValue(attrStartColor);
                    String centerColor = attrCenterColor == null? null : translateValue(attrCenterColor);
                    String endColor = attrEndColor == null? "0" : translateValue(attrEndColor);
                    String orientation = attrOrientation == null? "GradientDrawable.Orientation." + ORIENTATION[0] : translateValue(attrOrientation);
                    javaBlock += "int[] colors = new int[] {" + startColor +
                            (centerColor == null? ", " : ", " + centerColor + ", ") +
                            endColor + "};\n";
                    javaBlock += "GradientDrawable " + getRoot().getObjectName() + " = new GradientDrawable(" +
                            orientation + ", colors);\n";
                return javaBlock;
            } else if (n.getLabelName().equals("solid")) {
                    Attribute solidcolor = n.findAttrByName("android:color");
                    javaBlock += "GradientDrawable " + getRoot().getObjectName() + " = new GradientDrawable();\n";
                    javaBlock += getRoot().getObjectName() + ".setColor(" + translateAttribute(solidcolor, n) + ");\n";
                return javaBlock;
            }
        }
        throw new AX2JException(AX2JException.ATTRIBUTE_NOT_FOUND, "no gradient nor solid");
    }

    public class SpecialTranslator {
        private AX2JNode node;
        private List<Attribute> attrList;
        
        public SpecialTranslator(AX2JNode node) {
            this.node = node;
            this.attrList = node.getAttributes();
        }
        
        public String translate(Attribute attr) throws AX2JException {
            String attrName = attr.getQualifiedName();
            if (node.getLabelName().equals("gradient") && 
                    (attrName.equals("android:startColor") ||
                    attrName.equals("android:centerColor") ||
                    attrName.equals("android:endColor") ||
                    attrName.equals("android:angle"))) {
                return "";
            } else if (node.getLabelName().equals("solid")) {
                return "";
            }
            throw new AX2JException(AX2JException.METHOD_NOT_FOUND, attr.getQualifiedName());
        }
        
        private Attribute findAttrByName(String attrName) {
            for (Attribute a : attrList) {
                if (a.getQualifiedName().equals(attrName)) {
                    return a;
                }
            }
            return null;
        }
    }
}
