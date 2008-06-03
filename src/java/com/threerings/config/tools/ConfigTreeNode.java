//
// $Id$

package com.threerings.config.tools;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.samskivert.util.Tuple;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ManagedConfig;

/**
 * A node in the config tree.
 */
public class ConfigTreeNode extends DefaultMutableTreeNode
    implements Exportable
{
    /**
     * Creates a new node with the specified partial name.
     *
     * @param config the configuration for this node, or <code>null</code> if this is a
     * folder node.
     */
    public ConfigTreeNode (String partialName, ManagedConfig config)
    {
        super(partialName, config == null);
        _config = config;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigTreeNode ()
    {
    }

    /**
     * Returns the full name of this node.
     */
    public String getName ()
    {
        String partialName = encode((String)userObject);
        String parentName = (parent == null) ? null : ((ConfigTreeNode)parent).getName();
        return (parentName == null) ? partialName : (parentName + "/" + partialName);
    }

    /**
     * Returns the configuration contained in this node, or <code>null</code> for none.
     */
    public ManagedConfig getConfig ()
    {
        return _config;
    }

    /**
     * Sets whether or not this node is expanded in the tree.
     */
    public void setExpanded (boolean expanded)
    {
        _expanded = expanded;
    }

    /**
     * Finds the insertion point (existing parent and new child) for the specified configuration.
     * The new child will contain the necessary intermediate nodes.
     */
    public Tuple<ConfigTreeNode, ConfigTreeNode> getInsertionPoint (
        ManagedConfig config, String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; insert in this node
            ConfigTreeNode child = new ConfigTreeNode(decode(name), config);
            return new Tuple<ConfigTreeNode, ConfigTreeNode>(this, child);

        } else {
            // find (or create) next component in path, pass on the config
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, null);
                child.insertConfig(config, name.substring(idx + 1));
                return new Tuple<ConfigTreeNode, ConfigTreeNode>(this, child);
            }
            return child.getInsertionPoint(config, name.substring(idx + 1));
        }
    }

    /**
     * Inserts the given configuration under this node, creating any required intermediate
     * nodes.
     */
    public void insertConfig (ManagedConfig config, String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; insert in this node
            ConfigTreeNode child = new ConfigTreeNode(decode(name), config);
            insert(child, getInsertionIndex(child));

        } else {
            // find (or create) next component in path, pass on the config
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, null);
                insert(child, getInsertionIndex(child));
            }
            child.insertConfig(config, name.substring(idx + 1));
        }
    }

    /**
     * Adds all configurations under this node to the supplied group.
     */
    public void addConfigs (ConfigGroup<ManagedConfig> group)
    {
        if (_config != null) {
            _config.setName(getName());
            group.addConfig(_config);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).addConfigs(group);
            }
        }
    }

    /**
     * Removes all configurations under this node from the supplied group.
     */
    public void removeConfigs (ConfigGroup<ManagedConfig> group)
    {
        if (_config != null) {
            group.removeConfig(_config);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).removeConfigs(group);
            }
        }
    }

    /**
     * Puts all of the configurations under this node into the supplied list.
     */
    public void getConfigs (ArrayList<ManagedConfig> configs)
    {
        if (_config != null) {
            configs.add(_config);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).getConfigs(configs);
            }
        }
    }

    /**
     * Clears out the unique ids of all configs under this node.
     */
    public void clearConfigIds ()
    {
        if (_config != null) {
            _config.setId(0);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).clearConfigIds();
            }
        }
    }

    /**
     * Finds and returns the node with the specified name (or <code>null</code> if it can't be
     * found).
     */
    public ConfigTreeNode getNode (String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; look for it in this node
            return (_childrenByName == null) ? null : _childrenByName.get(decode(name));

        } else {
            // find (or create) next component in path, and look for it there
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            return (child == null) ? null : child.getNode(name.substring(idx + 1));
        }
    }

    /**
     * Verifies that if this node contains any actual configurations, they're instances of
     * the supplied class.
     */
    public boolean verifyConfigClass (Class clazz)
    {
        if (_config != null) {
            return clazz.isInstance(_config);

        } else if (children != null) {
            for (Object child : children) {
                if (!((ConfigTreeNode)child).verifyConfigClass(clazz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Finds an unused name for a child of this node from the specified base.
     */
    public String findNameForChild (String base)
    {
        if (_childrenByName == null || !_childrenByName.containsKey(base)) {
            return base;
        }
        for (int ii = 2;; ii++) {
            String name = base + " (" + ii + ")";
            if (!_childrenByName.containsKey(name)) {
                return name;
            }
        }
    }

    /**
     * Returns the index at which the specified node should be inserted to maintain the sort
     * order.
     */
    public int getInsertionIndex (ConfigTreeNode child)
    {
        if (children == null) {
            return 0;
        }
        String name = (String)child.getUserObject();
        boolean folder = child.getAllowsChildren();
        for (int ii = 0, nn = children.size(); ii < nn; ii++) {
            ConfigTreeNode ochild = (ConfigTreeNode)children.get(ii);
            String oname = (String)ochild.getUserObject();
            boolean ofolder = ochild.getAllowsChildren();
            if ((folder == ofolder) ? (name.compareTo(oname) <= 0) : folder) {
                return ii;
            }
        }
        return children.size();
    }

    /**
     * Expands paths according to the {@link #_expanded} field.
     */
    public void expandPaths (JTree tree)
    {
        if (_expanded) {
            tree.expandPath(new TreePath(getPath()));
        }
        if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).expandPaths(tree);
            }
        }
    }

    /**
     * Writes the exportable fields of the object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();
        out.write("name", (String)userObject, (String)null);
        out.write("parent", parent, null, MutableTreeNode.class);
        out.write("children", children, null, Vector.class);
    }

    /**
     * Reads the exportable fields of the object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        userObject = in.read("name", (String)null);
        parent = in.read("parent", null, MutableTreeNode.class);
        children = in.read("children", null, Vector.class);
        if (children != null) {
            _childrenByName = new HashMap<String, ConfigTreeNode>();
            for (Object child : children) {
                ConfigTreeNode node = (ConfigTreeNode)child;
                _childrenByName.put((String)node.getUserObject(), node);
            }
        }
        allowsChildren = (_config == null);
    }

    @Override // documentation inherited
    public void insert (MutableTreeNode child, int index)
    {
        super.insert(child, index);
        if (_childrenByName == null) {
            _childrenByName = new HashMap<String, ConfigTreeNode>();
        }
        ConfigTreeNode node = (ConfigTreeNode)child;
        _childrenByName.put((String)node.getUserObject(), node);
    }

    @Override // documentation inherited
    public void remove (int index)
    {
        ConfigTreeNode child = (ConfigTreeNode)children.get(index);
        _childrenByName.remove((String)child.getUserObject());
        if (_childrenByName.isEmpty()) {
            _childrenByName = null;
        }
        super.remove(index);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        ConfigTreeNode cnode = (ConfigTreeNode)super.clone();
        cnode.parent = null;
        if (_config != null) {
            cnode._config = (ManagedConfig)_config.clone();

        } else if (children != null) {
            cnode.children = new Vector();
            for (int ii = 0, nn = children.size(); ii < nn; ii++) {
                ConfigTreeNode child = (ConfigTreeNode)children.get(ii);
                cnode.insert((ConfigTreeNode)child.clone(), ii);
            }
        }
        return cnode;
    }

    /**
     * Encodes a partial name for use in a path name.
     */
    protected static String encode (String name)
    {
        return (name == null) ? null : name.replace("/", SLASH_REPLACEMENT);
    }

    /**
     * Decodes a name encoded with {@link #encode}.
     */
    protected static String decode (String name)
    {
        return (name == null) ? null : name.replace(SLASH_REPLACEMENT, "/");
    }

    /** The configuration contained in this node, if any. */
    protected ManagedConfig _config;

    /** Whether or not this node is expanded in the tree. */
    protected boolean _expanded;

    /** The children of this node, mapped by (partial) name. */
    protected transient HashMap<String, ConfigTreeNode> _childrenByName;

    /** Because we use slashes as name delimiters, any slashes in the name must be replaced. */
    protected static final String SLASH_REPLACEMENT = "%SLASH%";
}
