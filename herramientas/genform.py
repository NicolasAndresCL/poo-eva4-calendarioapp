"""Generador de .form (NetBeans) + .java con initComponents() coherente, GroupLayout por filas."""
from xml.sax.saxutils import escape

J = "javax.swing.GroupLayout"
FILL = "fill"


def C(cls, name, *props, w=None, h=None, ev=None, tp=None):
    return dict(kind="comp", cls=cls, name=name, props=list(props), w=w, h=h, ev=ev, tp=tp)


def P(name, rows, *props, w=None, h=None):
    return dict(kind="panel", cls="javax.swing.JPanel", name=name, rows=rows, props=list(props), w=w, h=h, ev=None, tp=None)


def S(name, child, w=None, h=None):
    return dict(kind="scroll", cls="javax.swing.JScrollPane", name=name, child=child, props=[], w=w, h=h, ev=None, tp=None)


def R(*items, align="baseline", gap="related"):
    return dict(items=list(items), align=align, gap=gap)


def xa(v):
    return escape(str(v), {'"': "&quot;"})


def jstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


# ---------- propiedades ----------
def prop_xml(p, ind):
    k, v = p
    s = " " * ind
    if k in ("text", "toolTipText"):
        return f'{s}<Property name="{k}" type="java.lang.String" value="{xa(v)}"/>'
    if k in ("foreground", "background"):
        r, g, b = v
        return (f'{s}<Property name="{k}" type="java.awt.Color" editor="org.netbeans.beaninfo.editors.ColorEditor">\n'
                f'{s}  <Color blue="{b:x}" green="{g:x}" red="{r:x}" type="rgb"/>\n{s}</Property>')
    if k == "font":
        n, st, sz = v
        return (f'{s}<Property name="font" type="java.awt.Font" editor="org.netbeans.beaninfo.editors.FontEditor">\n'
                f'{s}  <Font name="{n}" size="{sz}" style="{st}"/>\n{s}</Property>')
    if k in ("editable", "enabled", "opaque", "stringPainted"):
        return f'{s}<Property name="{k}" type="boolean" value="{str(v).lower()}"/>'
    if k in ("horizontalAlignment", "selectionMode", "columns", "rows"):
        return f'{s}<Property name="{k}" type="int" value="{v}"/>'
    if k == "combo":
        items = "\n".join(f'{s}    <StringItem index="{i}" value="{xa(t)}"/>' for i, t in enumerate(v))
        return (f'{s}<Property name="model" type="javax.swing.ComboBoxModel" editor="org.netbeans.modules.form.editors2.ComboBoxModelEditor">\n'
                f'{s}  <StringArray count="{len(v)}">\n{items}\n{s}  </StringArray>\n{s}</Property>')
    if k == "spinner":
        i0, mn, mx, st = v
        return (f'{s}<Property name="model" type="javax.swing.SpinnerModel" editor="org.netbeans.modules.form.editors2.SpinnerModelEditor">\n'
                f'{s}  <SpinnerModel initial="{i0}" maximum="{mx}" minimum="{mn}" numberType="java.lang.Integer" stepSize="{st}" type="number"/>\n{s}</Property>')
    if k == "table":
        cols = "\n".join(f'{s}    <Column editable="false" title="{xa(t)}" type="java.lang.Object"/>' for t in v)
        return (f'{s}<Property name="model" type="javax.swing.table.TableModel" editor="org.netbeans.modules.form.editors2.TableModelEditor">\n'
                f'{s}  <Table columnCount="{len(v)}" rowCount="0">\n{cols}\n{s}  </Table>\n{s}</Property>')
    if k == "border":
        return (f'{s}<Property name="border" type="javax.swing.border.Border" editor="org.netbeans.modules.form.editors2.BorderEditor">\n'
                f'{s}  <Border info="org.netbeans.modules.form.compat2.border.TitledBorderInfo">\n'
                f'{s}    <TitledBorder title="{xa(v)}"/>\n{s}  </Border>\n{s}</Property>')
    raise ValueError(k)


def prop_java(n, p):
    k, v = p
    i = " " * 8
    if k == "text":
        return f"{i}{n}.setText({jstr(v)});"
    if k == "toolTipText":
        return f"{i}{n}.setToolTipText({jstr(v)});"
    if k in ("foreground", "background"):
        return f"{i}{n}.set{k[0].upper()}{k[1:]}(new java.awt.Color({v[0]}, {v[1]}, {v[2]}));"
    if k == "font":
        return f'{i}{n}.setFont(new java.awt.Font("{v[0]}", {v[1]}, {v[2]})); // NOI18N'
    if k in ("editable", "enabled", "opaque", "stringPainted"):
        return f"{i}{n}.set{k[0].upper()}{k[1:]}({str(v).lower()});"
    if k == "horizontalAlignment":
        return f"{i}{n}.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);"
    if k in ("columns", "rows"):
        return f"{i}{n}.set{k[0].upper()}{k[1:]}({v});"
    if k == "selectionMode":
        return f"{i}{n}.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);"
    if k == "combo":
        return f"{i}{n}.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {{ {', '.join(jstr(t) for t in v)} }}));"
    if k == "spinner":
        return f"{i}{n}.setModel(new javax.swing.SpinnerNumberModel({v[0]}, {v[1]}, {v[2]}, {v[3]}));"
    if k == "border":
        return f"{i}{n}.setBorder(javax.swing.BorderFactory.createTitledBorder({jstr(v)}));"
    if k == "table":
        cols = ", ".join(jstr(t) for t in v)
        edits = ", ".join("false" for _ in v)
        return (f"{i}{n}.setModel(new javax.swing.table.DefaultTableModel(\n"
                f"{i}    new Object [][] {{\n\n{i}    }},\n"
                f"{i}    new String [] {{\n{i}        {cols}\n{i}    }}\n"
                f"{i}) {{\n{i}    boolean[] canEdit = new boolean [] {{\n{i}        {edits}\n{i}    }};\n\n"
                f"{i}    public boolean isCellEditable(int rowIndex, int columnIndex) {{\n"
                f"{i}        return canEdit [columnIndex];\n{i}    }}\n{i}}});")
    raise ValueError(k)


# ---------- arbol de layout ----------
def size_of(spec, dim):
    v = spec["w"] if dim == 0 else spec["h"]
    return v


def build_tree(rows, dim):
    if dim == 0:
        par_rows = []
        for r in rows:
            its = r["items"]
            if len(its) == 1:
                par_rows.append(("comp", its[0]["name"], size_of(its[0], 0)))
            else:
                seq = []
                for k, it in enumerate(its):
                    if k:
                        seq.append(("gap", "related"))
                    seq.append(("comp", it["name"], size_of(it, 0)))
                par_rows.append(("seq", seq))
        return ("par", 0, [("seq", [("gap", "container"), ("par", 0, par_rows), ("gap", "container")])])
    seq = [("gap", "container")]
    for k, r in enumerate(rows):
        if k:
            seq.append(("gap", r["gap"]))
        its = r["items"]
        if len(its) == 1:
            seq.append(("comp", its[0]["name"], size_of(its[0], 1)))
        else:
            al = 3 if r["align"] == "baseline" else 0
            seq.append(("par", al, [("comp", it["name"], size_of(it, 1)) for it in its]))
    seq.append(("gap", "containerFill"))
    return ("par", 0, [("seq", seq)])


def tree_xml(node, ind, in_par_align=None):
    s = " " * ind
    t = node[0]
    al = f' alignment="{in_par_align}"' if in_par_align is not None else ""
    if t == "gap":
        return {"container": f'{s}<EmptySpace max="-2" attributes="0"/>',
                "related": f'{s}<EmptySpace max="-2" attributes="0"/>',
                "unrelated": f'{s}<EmptySpace type="unrelated" max="-2" attributes="0"/>',
                "containerFill": f'{s}<EmptySpace max="32767" attributes="0"/>'}[node[1]]
    if t == "comp":
        _, name, sz = node
        if sz == FILL:
            return f'{s}<Component id="{name}"{al} max="32767" attributes="0"/>'
        if sz is None:
            return f'{s}<Component id="{name}"{al} min="-2" max="-2" attributes="0"/>'
        return f'{s}<Component id="{name}"{al} min="-2" pref="{sz}" max="-2" attributes="0"/>'
    if t == "seq":
        inner = "\n".join(tree_xml(c, ind + 4) for c in node[1])
        return f'{s}<Group type="102"{al} attributes="0">\n{inner}\n{s}</Group>'
    if t == "par":
        inner = "\n".join(tree_xml(c, ind + 4, node[1]) for c in node[2])
        return f'{s}<Group type="103"{al} groupAlignment="{node[1]}" attributes="0">\n{inner}\n{s}</Group>'


def tree_java(node, lay, ind, first=False, last=False):
    s = " " * ind
    t = node[0]
    if t == "gap":
        k = node[1]
        if k == "containerFill":
            return f"{s}.addContainerGap({J}.DEFAULT_SIZE, Short.MAX_VALUE)"
        if k == "container" and (first or last):
            return f"{s}.addContainerGap()"
        place = "UNRELATED" if k == "unrelated" else "RELATED"
        return f"{s}.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.{place})"
    if t == "comp":
        _, name, sz = node
        if sz == FILL:
            return f"{s}.addComponent({name}, {J}.DEFAULT_SIZE, {J}.DEFAULT_SIZE, Short.MAX_VALUE)"
        if sz is None:
            return f"{s}.addComponent({name}, {J}.PREFERRED_SIZE, {J}.DEFAULT_SIZE, {J}.PREFERRED_SIZE)"
        return f"{s}.addComponent({name}, {J}.PREFERRED_SIZE, {sz}, {J}.PREFERRED_SIZE)"
    return f"{s}.addGroup({group_java(node, lay, ind)})"


def group_java(node, lay, ind):
    s = " " * ind
    if node[0] == "seq":
        ch = node[1]
        lines = [tree_java(c, lay, ind + 4, k == 0, k == len(ch) - 1) for k, c in enumerate(ch)]
        return f"{lay}.createSequentialGroup()\n" + "\n".join(lines)
    a = {0: "LEADING", 3: "BASELINE"}[node[1]]
    lines = [tree_java(c, lay, ind + 4) for c in node[2]]
    return f"{lay}.createParallelGroup(javax.swing.GroupLayout.Alignment.{a})\n" + "\n".join(lines)


def layout_xml(rows, ind):
    s = " " * ind
    h = tree_xml(build_tree(rows, 0), ind + 6)
    v = tree_xml(build_tree(rows, 1), ind + 6)
    return (f"{s}<Layout>\n{s}  <DimensionLayout dim=\"0\">\n{h}\n{s}  </DimensionLayout>\n"
            f"{s}  <DimensionLayout dim=\"1\">\n{v}\n{s}  </DimensionLayout>\n{s}</Layout>")


def layout_java(rows, lay, target):
    i = " " * 8
    out = [f"{i}javax.swing.GroupLayout {lay} = new javax.swing.GroupLayout({target});",
           f"{i}{target}.setLayout({lay});"]
    for dim, meth in ((0, "setHorizontalGroup"), (1, "setVerticalGroup")):
        out.append(f"{i}{lay}.{meth}(\n{i}    {group_java(build_tree(rows, dim), lay, 12)}\n{i});")
    return "\n".join(out)


# ---------- componentes ----------
def comp_xml(c, ind):
    s = " " * ind
    tag = "Container" if c["kind"] in ("panel", "scroll") else "Component"
    out = [f'{s}<{tag} class="{c["cls"]}" name="{c["name"]}">']
    if c["props"]:
        out.append(f"{s}  <Properties>")
        out += [prop_xml(p, ind + 4) for p in c["props"]]
        out.append(f"{s}  </Properties>")
    if c["ev"]:
        out += [f"{s}  <Events>",
                f'{s}    <EventHandler event="actionPerformed" listener="java.awt.event.ActionListener" parameters="java.awt.event.ActionEvent" handler="{c["ev"]}"/>',
                f"{s}  </Events>"]
    if c["tp"]:
        out += [f"{s}  <AuxValues>",
                f'{s}    <AuxValue name="JavaCodeGenerator_TypeParameters" type="java.lang.String" value="&lt;{c["tp"]}&gt;"/>',
                f"{s}  </AuxValues>"]
    if c["kind"] == "scroll":
        out += [f"{s}  <AuxValues>",
                f'{s}    <AuxValue name="autoScrollPane" type="java.lang.Boolean" value="true"/>',
                f"{s}  </AuxValues>", "",
                f'{s}  <Layout class="org.netbeans.modules.form.compat2.layouts.support.JScrollPaneSupportLayout"/>',
                f"{s}  <SubComponents>", comp_xml(c["child"], ind + 4), f"{s}  </SubComponents>"]
    if c["kind"] == "panel":
        out += ["", layout_xml(c["rows"], ind + 2), f"{s}  <SubComponents>"]
        out += [comp_xml(it, ind + 4) for r in c["rows"] for it in r["items"]]
        out.append(f"{s}  </SubComponents>")
    out.append(f"{s}</{tag}>")
    return "\n".join(out)


def all_comps(rows):
    for r in rows:
        for it in r["items"]:
            yield it
            if it["kind"] == "panel":
                yield from all_comps(it["rows"])
            if it["kind"] == "scroll":
                yield it["child"]


def comp_java(c):
    i = " " * 8
    out = [prop_java(c["name"], p) for p in c["props"]]
    if c["ev"]:
        out.append(f"{i}{c['name']}.addActionListener(this::{c['ev']});")
    if c["kind"] == "scroll":
        out.append(comp_java(c["child"]))
        out.append(f"{i}{c['name']}.setViewportView({c['child']['name']});")
    if c["kind"] == "panel":
        for r in c["rows"]:
            for it in r["items"]:
                out.append(comp_java(it))
        out.append("")
        out.append(layout_java(c["rows"], c["name"] + "Layout", c["name"]))
    return "\n".join(x for x in out if x is not None) + "\n"


def generate(win, java_path, form_path):
    rows = win["rows"]
    comps = list(all_comps(rows))
    dialog = win["type"] == "dialog"
    info = "JDialogFormInfo" if dialog else "JFrameFormInfo"
    close = 2 if dialog else 3
    form = [f'<?xml version="1.0" encoding="UTF-8" ?>\n',
            f'<Form version="1.3" maxVersion="1.9" type="org.netbeans.modules.form.forminfo.{info}">',
            "  <Properties>",
            f'    <Property name="defaultCloseOperation" type="int" value="{close}"/>',
            f'    <Property name="title" type="java.lang.String" value="{xa(win["title"])}"/>',
            "  </Properties>",
            "  <SyntheticProperties>",
            '    <SyntheticProperty name="formSizePolicy" type="int" value="1"/>',
            '    <SyntheticProperty name="generateCenter" type="boolean" value="false"/>',
            "  </SyntheticProperties>",
            "  <AuxValues>"]
    aux = [("FormSettings_autoResourcing", "Integer", "0"), ("FormSettings_autoSetComponentName", "Boolean", "false"),
           ("FormSettings_generateFQN", "Boolean", "true"), ("FormSettings_generateMnemonicsCode", "Boolean", "false"),
           ("FormSettings_i18nAutoMode", "Boolean", "false"), ("FormSettings_layoutCodeTarget", "Integer", "1"),
           ("FormSettings_listenerGenerationStyle", "Integer", "3"), ("FormSettings_variablesLocal", "Boolean", "false"),
           ("FormSettings_variablesModifier", "Integer", "2")]
    form += [f'    <AuxValue name="{n}" type="java.lang.{t}" value="{v}"/>' for n, t, v in aux]
    form += ["  </AuxValues>", "", layout_xml(rows, 2), "  <SubComponents>"]
    form += [comp_xml(it, 4) for r in rows for it in r["items"]]
    form += ["  </SubComponents>", "</Form>", ""]
    with open(form_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(form))

    i = " " * 8
    init = [f"{i}{c['name']} = new {c['cls']}{'<>' if c['tp'] else ''}();" for c in comps]
    const = "DISPOSE_ON_CLOSE" if dialog else "EXIT_ON_CLOSE"
    init += ["", f"{i}setDefaultCloseOperation(javax.swing.WindowConstants.{const});",
             f"{i}setTitle({jstr(win['title'])});", ""]
    for r in rows:
        for it in r["items"]:
            init.append(comp_java(it))
    init.append(layout_java(rows, "layout", "getContentPane()").replace(
        "new javax.swing.GroupLayout(getContentPane());\n        getContentPane().setLayout(layout);",
        "new javax.swing.GroupLayout(getContentPane());\n        getContentPane().setLayout(layout);"))
    init += ["", f"{i}pack();"]

    decls = sorted(comps, key=lambda c: c["name"])
    var = "\n".join(f"    private {c['cls']}{'<' + c['tp'] + '>' if c['tp'] else ''} {c['name']};" for c in decls)

    java = (win["head"] +
            win["body1"] +
            "    /**\n     * This method is called from within the constructor to initialize the form.\n"
            "     * WARNING: Do NOT modify this code. The content of this method is always\n"
            "     * regenerated by the Form Editor.\n     */\n"
            '    @SuppressWarnings("unchecked")\n'
            '    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents\n'
            "    private void initComponents() {\n\n" + "\n".join(init) +
            "\n    }// </editor-fold>//GEN-END:initComponents\n\n" +
            win["body2"] +
            "\n    // Variables declaration - do not modify//GEN-BEGIN:variables\n" + var +
            "\n    // End of variables declaration//GEN-END:variables\n}\n")
    with open(java_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(java)
