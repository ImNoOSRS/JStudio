package com.tonic.ui.browser;

import com.tonic.parser.ConstPool;
import com.tonic.parser.attribute.*;
import com.tonic.parser.constpool.*;
import com.tonic.ui.theme.JStudioTheme;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class DetailsPanel extends JPanel {

    private final JTextArea detailsArea;
    private final JToggleButton hexToggle;
    private boolean showHex = false;

    private Item<?> currentItem;
    private int currentIndex;
    private Attribute currentAttribute;
    private ConstPool constPool;

    public DetailsPanel() {
        setLayout(new BorderLayout());
        setBackground(JStudioTheme.getBgTertiary());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.setBackground(JStudioTheme.getBgSecondary());

        hexToggle = new JToggleButton("Hex");
        hexToggle.setFont(JStudioTheme.getUIFont(10));
        hexToggle.setFocusable(false);
        hexToggle.addActionListener(e -> {
            showHex = hexToggle.isSelected();
            refresh();
        });
        toolbar.add(hexToggle);

        add(toolbar, BorderLayout.NORTH);

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(JStudioTheme.getCodeFont(11));
        detailsArea.setBackground(JStudioTheme.getBgTertiary());
        detailsArea.setForeground(JStudioTheme.getTextPrimary());
        detailsArea.setCaretColor(JStudioTheme.getTextPrimary());
        detailsArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(detailsArea, BorderLayout.CENTER);
    }

    public void clear() {
        currentItem = null;
        currentAttribute = null;
        detailsArea.setText("");
    }

    public void showItem(Item<?> item, int index, ConstPool constPool) {
        this.currentItem = item;
        this.currentIndex = index;
        this.currentAttribute = null;
        this.constPool = constPool;
        refresh();
    }

    public void showAttribute(Attribute attribute, String context, ConstPool constPool) {
        this.currentAttribute = attribute;
        this.currentItem = null;
        this.constPool = constPool;
        refreshAttribute(context);
    }

    private void refresh() {
        if (currentItem == null) {
            detailsArea.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Constant Pool Entry #").append(currentIndex).append(" ===\n\n");
        sb.append("Type: ").append(getTypeName(currentItem)).append("\n");
        sb.append("Tag:  ").append(currentItem.getType() & 0xFF).append("\n\n");

        sb.append("--- Value ---\n");
        appendItemDetails(sb, currentItem);

        if (showHex) {
            sb.append("\n--- Raw Bytes ---\n");
            appendHexDump(sb, currentItem);
        }

        detailsArea.setText(sb.toString());
        detailsArea.setCaretPosition(0);
    }

    private void refreshAttribute(String context) {
        if (currentAttribute == null) {
            detailsArea.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Attribute ===\n\n");
        sb.append("Name: ").append(getAttributeTypeName(currentAttribute)).append("\n");
        sb.append("Context: ").append(context).append("\n\n");

        sb.append("--- Details ---\n");
        appendAttributeDetails(sb, currentAttribute);

        detailsArea.setText(sb.toString());
        detailsArea.setCaretPosition(0);
    }

    private void appendItemDetails(StringBuilder sb, Item<?> item) {
        try {
            if (item instanceof Utf8Item) {
                String val = ((Utf8Item) item).getValue();
                sb.append("Length: ").append(val.length()).append(" characters\n\n");
                sb.append("Value:\n").append(val).append("\n");
                return;
            }
            if (item instanceof IntegerItem) {
                int val = ((IntegerItem) item).getValue();
                sb.append("Decimal: ").append(val).append("\n");
                sb.append("Hex:     0x").append(Integer.toHexString(val)).append("\n");
                sb.append("Binary:  ").append(Integer.toBinaryString(val)).append("\n");
                return;
            }
            if (item instanceof LongItem) {
                long val = ((LongItem) item).getValue();
                sb.append("Decimal: ").append(val).append("L\n");
                sb.append("Hex:     0x").append(Long.toHexString(val)).append("\n");
                return;
            }
            if (item instanceof FloatItem) {
                float val = ((FloatItem) item).getValue();
                sb.append("Value: ").append(val).append("f\n");
                sb.append("Raw bits: 0x").append(Integer.toHexString(Float.floatToRawIntBits(val))).append("\n");
                return;
            }
            if (item instanceof DoubleItem) {
                double val = ((DoubleItem) item).getValue();
                sb.append("Value: ").append(val).append("d\n");
                sb.append("Raw bits: 0x").append(Long.toHexString(Double.doubleToRawLongBits(val))).append("\n");
                return;
            }
            if (item instanceof ClassRefItem) {
                ClassRefItem classRef = (ClassRefItem) item;
                int nameIdx = classRef.getNameIndex();
                sb.append("Name index: #").append(nameIdx).append("\n");
                sb.append("Resolved:   ").append(getUtf8(nameIdx)).append("\n");
                return;
            }
            if (item instanceof StringRefItem) {
                StringRefItem stringRef = (StringRefItem) item;
                int utf8Idx = stringRef.getValue();
                sb.append("UTF8 index: #").append(utf8Idx).append("\n");
                sb.append("Value:      \"").append(getUtf8(utf8Idx)).append("\"\n");
                return;
            }
            if (item instanceof FieldRefItem) {
                FieldRefItem ref = (FieldRefItem) item;
                appendMemberRefDetails(sb, "Field", ref.getValue().getClassIndex(),
                        ref.getValue().getNameAndTypeIndex());
                return;
            }
            if (item instanceof MethodRefItem) {
                MethodRefItem ref = (MethodRefItem) item;
                appendMemberRefDetails(sb, "Method", ref.getValue().getClassIndex(),
                        ref.getValue().getNameAndTypeIndex());
                return;
            }
            if (item instanceof InterfaceRefItem) {
                InterfaceRefItem ref = (InterfaceRefItem) item;
                appendMemberRefDetails(sb, "Interface Method", ref.getValue().getClassIndex(),
                        ref.getValue().getNameAndTypeIndex());
                return;
            }
            if (item instanceof NameAndTypeRefItem) {
                NameAndTypeRefItem nat = (NameAndTypeRefItem) item;
                int nameIdx = nat.getValue().getNameIndex();
                int descIdx = nat.getValue().getDescriptorIndex();
                sb.append("Name index:       #").append(nameIdx).append("\n");
                sb.append("Descriptor index: #").append(descIdx).append("\n\n");
                sb.append("Name:       ").append(getUtf8(nameIdx)).append("\n");
                sb.append("Descriptor: ").append(getUtf8(descIdx)).append("\n");
                return;
            }
            if (item instanceof MethodHandleItem) {
                MethodHandleItem mh = (MethodHandleItem) item;
                int kind = mh.getValue().getReferenceKind();
                int refIdx = mh.getValue().getReferenceIndex();
                sb.append("Reference kind:  ").append(kind).append(" (").append(getHandleKindName(kind)).append(")\n");
                sb.append("Reference index: #").append(refIdx).append("\n");
                return;
            }
            if (item instanceof MethodTypeItem) {
                MethodTypeItem mt = (MethodTypeItem) item;
                int descIdx = mt.getValue();
                sb.append("Descriptor index: #").append(descIdx).append("\n");
                sb.append("Descriptor:       ").append(getUtf8(descIdx)).append("\n");
                return;
            }
            if (item instanceof InvokeDynamicItem) {
                InvokeDynamicItem indy = (InvokeDynamicItem) item;
                int bsmIdx = indy.getValue().getBootstrapMethodAttrIndex();
                int natIdx = indy.getValue().getNameAndTypeIndex();
                sb.append("Bootstrap method index: ").append(bsmIdx).append("\n");
                sb.append("NameAndType index:      #").append(natIdx).append("\n");
                appendNameAndType(sb, natIdx);
                return;
            }
            if (item instanceof ConstantDynamicItem) {
                ConstantDynamicItem cd = (ConstantDynamicItem) item;
                int bsmIdx = cd.getValue().getBootstrapMethodAttrIndex();
                int natIdx = cd.getValue().getNameAndTypeIndex();
                sb.append("Bootstrap method index: ").append(bsmIdx).append("\n");
                sb.append("NameAndType index:      #").append(natIdx).append("\n");
                appendNameAndType(sb, natIdx);
                return;
            }
            if (item instanceof PackageItem) {
                PackageItem pkg = (PackageItem) item;
                int nameIdx = pkg.getValue();
                sb.append("Name index: #").append(nameIdx).append("\n");
                sb.append("Package:    ").append(getUtf8(nameIdx)).append("\n");
                return;
            }
            if (item instanceof ModuleItem) {
                ModuleItem mod = (ModuleItem) item;
                int nameIdx = mod.getValue();
                sb.append("Name index: #").append(nameIdx).append("\n");
                sb.append("Module:     ").append(getUtf8(nameIdx)).append("\n");
                return;
            }

            sb.append("Value: ").append(item.getValue()).append("\n");
        } catch (Exception e) {
            sb.append("Error reading item: ").append(e.getMessage()).append("\n");
        }
    }

    private void appendMemberRefDetails(StringBuilder sb, String kind, int classIdx, int natIdx) {
        sb.append(kind).append(" Reference\n\n");
        sb.append("Class index:       #").append(classIdx).append("\n");
        sb.append("NameAndType index: #").append(natIdx).append("\n\n");

        try {
            Item<?> classItem = constPool.getItem(classIdx);
            if (classItem instanceof ClassRefItem) {
                int nameIdx = ((ClassRefItem) classItem).getNameIndex();
                sb.append("Class: ").append(getUtf8(nameIdx)).append("\n");
            }
            appendNameAndType(sb, natIdx);
        } catch (Exception e) {
            sb.append("Error resolving: ").append(e.getMessage()).append("\n");
        }
    }

    private void appendNameAndType(StringBuilder sb, int natIdx) {
        try {
            Item<?> natItem = constPool.getItem(natIdx);
            if (natItem instanceof NameAndTypeRefItem) {
                NameAndTypeRefItem nat = (NameAndTypeRefItem) natItem;
                sb.append("Name:       ").append(getUtf8(nat.getValue().getNameIndex())).append("\n");
                sb.append("Descriptor: ").append(getUtf8(nat.getValue().getDescriptorIndex())).append("\n");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void appendAttributeDetails(StringBuilder sb, Attribute attr) {
        if (attr instanceof CodeAttribute) {
            CodeAttribute code = (CodeAttribute) attr;
            sb.append("Max stack:  ").append(code.getMaxStack()).append("\n");
            sb.append("Max locals: ").append(code.getMaxLocals()).append("\n");
            sb.append("Code length: ").append(code.getCode() != null ? code.getCode().length : 0).append(" bytes\n");
            sb.append("Exception handlers: ").append(code.getExceptionTable() != null ? code.getExceptionTable().size() : 0).append("\n");
            if (code.getAttributes() != null) {
                sb.append("Nested attributes: ").append(code.getAttributes().size()).append("\n");
                for (Attribute nested : code.getAttributes()) {
                    sb.append("  - ").append(getAttributeTypeName(nested)).append("\n");
                }
            }
            return;
        }
        if (attr instanceof SourceFileAttribute) {
            SourceFileAttribute sf = (SourceFileAttribute) attr;
            sb.append("Source file index: #").append(sf.getSourceFileIndex()).append("\n");
            sb.append("Source file: ").append(getUtf8(sf.getSourceFileIndex())).append("\n");
            return;
        }
        if (attr instanceof ConstantValueAttribute) {
            ConstantValueAttribute cv = (ConstantValueAttribute) attr;
            sb.append("Value index: #").append(cv.getConstantValueIndex()).append("\n");
            return;
        }
        if (attr instanceof ExceptionsAttribute) {
            ExceptionsAttribute ex = (ExceptionsAttribute) attr;
            sb.append("Throws ").append(ex.getExceptionIndexTable().size()).append(" exception(s):\n");
            for (int idx : ex.getExceptionIndexTable()) {
                sb.append("  - #").append(idx).append("\n");
            }
            return;
        }
        if (attr instanceof InnerClassesAttribute) {
            InnerClassesAttribute ic = (InnerClassesAttribute) attr;
            sb.append("Inner classes: ").append(ic.getClasses().size()).append("\n");
            return;
        }
        if (attr instanceof LineNumberTableAttribute) {
            LineNumberTableAttribute lnt = (LineNumberTableAttribute) attr;
            sb.append("Entries: ").append(lnt.getLineNumberTable().size()).append("\n");
            int count = 0;
            for (var entry : lnt.getLineNumberTable()) {
                sb.append("  pc=").append(entry.getStartPc()).append(" → line ").append(entry.getLineNumber()).append("\n");
                if (++count >= 20) {
                    sb.append("  ... (").append(lnt.getLineNumberTable().size() - 20).append(" more)\n");
                    break;
                }
            }
            return;
        }
        if (attr instanceof LocalVariableTableAttribute) {
            LocalVariableTableAttribute lvt = (LocalVariableTableAttribute) attr;
            sb.append("Variables: ").append(lvt.getLocalVariableTable().size()).append("\n");
            int count = 0;
            for (var entry : lvt.getLocalVariableTable()) {
                String name = getUtf8(entry.getNameIndex());
                String desc = getUtf8(entry.getDescriptorIndex());
                sb.append("  slot ").append(entry.getIndex()).append(": ").append(name).append(" : ").append(desc).append("\n");
                if (++count >= 15) {
                    sb.append("  ... (").append(lvt.getLocalVariableTable().size() - 15).append(" more)\n");
                    break;
                }
            }
            return;
        }
        if (attr instanceof SignatureAttribute) {
            SignatureAttribute sig = (SignatureAttribute) attr;
            sb.append("Signature index: #").append(sig.getSignatureIndex()).append("\n");
            sb.append("Signature: ").append(getUtf8(sig.getSignatureIndex())).append("\n");
            return;
        }
        if (attr instanceof DeprecatedAttribute) {
            sb.append("This element is deprecated.\n");
            return;
        }
        if (attr instanceof SyntheticAttribute) {
            sb.append("This element is synthetic (compiler-generated).\n");
            return;
        }

        sb.append("(Generic attribute - no detailed view available)\n");
    }

    private void appendHexDump(StringBuilder sb, Item<?> item) {
        sb.append("(Hex dump not available for this item type)\n");
    }

    private String getTypeName(Item<?> item) {
        if (item instanceof Utf8Item) return "CONSTANT_Utf8";
        if (item instanceof IntegerItem) return "CONSTANT_Integer";
        if (item instanceof FloatItem) return "CONSTANT_Float";
        if (item instanceof LongItem) return "CONSTANT_Long";
        if (item instanceof DoubleItem) return "CONSTANT_Double";
        if (item instanceof ClassRefItem) return "CONSTANT_Class";
        if (item instanceof StringRefItem) return "CONSTANT_String";
        if (item instanceof FieldRefItem) return "CONSTANT_Fieldref";
        if (item instanceof MethodRefItem) return "CONSTANT_Methodref";
        if (item instanceof InterfaceRefItem) return "CONSTANT_InterfaceMethodref";
        if (item instanceof NameAndTypeRefItem) return "CONSTANT_NameAndType";
        if (item instanceof MethodHandleItem) return "CONSTANT_MethodHandle";
        if (item instanceof MethodTypeItem) return "CONSTANT_MethodType";
        if (item instanceof ConstantDynamicItem) return "CONSTANT_Dynamic";
        if (item instanceof InvokeDynamicItem) return "CONSTANT_InvokeDynamic";
        if (item instanceof PackageItem) return "CONSTANT_Package";
        if (item instanceof ModuleItem) return "CONSTANT_Module";
        return "Unknown";
    }

    private String getHandleKindName(int kind) {
        switch (kind) {
            case 1: return "REF_getField";
            case 2: return "REF_getStatic";
            case 3: return "REF_putField";
            case 4: return "REF_putStatic";
            case 5: return "REF_invokeVirtual";
            case 6: return "REF_invokeStatic";
            case 7: return "REF_invokeSpecial";
            case 8: return "REF_newInvokeSpecial";
            case 9: return "REF_invokeInterface";
            default: return "Unknown";
        }
    }

    private String getUtf8(int index) {
        try {
            Item<?> item = constPool.getItem(index);
            if (item instanceof Utf8Item) {
                return ((Utf8Item) item).getValue();
            }
        } catch (Exception e) {
            // ignore
        }
        return "#" + index;
    }

    private String getAttributeTypeName(Attribute attr) {
        if (attr instanceof CodeAttribute) return "Code";
        if (attr instanceof ConstantValueAttribute) return "ConstantValue";
        if (attr instanceof StackMapTableAttribute) return "StackMapTable";
        if (attr instanceof ExceptionsAttribute) return "Exceptions";
        if (attr instanceof InnerClassesAttribute) return "InnerClasses";
        if (attr instanceof EnclosingMethodAttribute) return "EnclosingMethod";
        if (attr instanceof SyntheticAttribute) return "Synthetic";
        if (attr instanceof SignatureAttribute) return "Signature";
        if (attr instanceof SourceFileAttribute) return "SourceFile";
        if (attr instanceof SourceDebugExtensionAttribute) return "SourceDebugExtension";
        if (attr instanceof LineNumberTableAttribute) return "LineNumberTable";
        if (attr instanceof LocalVariableTableAttribute) return "LocalVariableTable";
        if (attr instanceof LocalVariableTypeTableAttribute) return "LocalVariableTypeTable";
        if (attr instanceof DeprecatedAttribute) return "Deprecated";
        if (attr instanceof RuntimeVisibleAnnotationsAttribute) return "RuntimeVisibleAnnotations";
        if (attr instanceof RuntimeInvisibleAnnotationsAttribute) return "RuntimeInvisibleAnnotations";
        if (attr instanceof RuntimeVisibleParameterAnnotationsAttribute) return "RuntimeVisibleParameterAnnotations";
        if (attr instanceof AnnotationDefaultAttribute) return "AnnotationDefault";
        if (attr instanceof MethodParametersAttribute) return "MethodParameters";
        if (attr instanceof BootstrapMethodsAttribute) return "BootstrapMethods";
        if (attr instanceof ModuleAttribute) return "Module";
        if (attr instanceof NestHostAttribute) return "NestHost";
        if (attr instanceof NestMembersAttribute) return "NestMembers";
        return "Attribute";
    }
}
