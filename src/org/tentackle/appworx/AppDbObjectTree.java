/**
 * Tentackle - a framework for java desktop applications
 * Copyright (C) 2001-2008 Harald Krake, harald@krake.de, +49 7722 9508-0
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// $Id: AppDbObjectTree.java 465 2009-07-19 15:18:09Z harald $


package org.tentackle.appworx;

import java.text.MessageFormat;
import org.tentackle.ui.FormHelper;
import org.tentackle.ui.FormInfo;
import org.tentackle.ui.FormTree;
import org.tentackle.util.FileTransferable;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/**
 * An extended {@link FormTree} that implements object navigation,
 * drag and drop, clipboard functionality for {@link AppDbObject}s,
 * and provides a context-sensitive popup menu,
 * 
 * @author harald
 */
public class AppDbObjectTree extends FormTree implements TreeWillExpandListener, DragSourceListener, DragGestureListener, DropTargetListener {
         

  
  /**
   * Helper method for applications to get the object of the parent node.
   * 
   * @param node the child node
   * @return the object of the parent node, null if none or no parent
   */
  public static Object getObjectInParentNode(DefaultMutableTreeNode node) {
    if (node != null) {
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
      if (parentNode != null) {
        Object object = parentNode.getUserObject();
        if (object instanceof AppDbTreeObject)  {
          return ((AppDbTreeObject)object).getObject();
        }
      }
    }
    return null;
  }
  
  
  
  
  private AppDbObject                             popupObject;            // object for popup
  private DefaultMutableTreeNode                  popupNode;              // node for popup
  private DefaultMutableTreeNode                  dragNode;               // dragged node
  private TreePath                                popupPath;              // path for popup
  private boolean                                 popupEnabled;           // true if popup allowed
  private AppDbObject                             dragObject;             // object dragged
  private Runnable                                openEditor;             // != null if object provides special editor
  private AppDbObjectTreeExtensionUsageToggleNode usageToggleNode;        // != null if usage toggle node available
  private JMenuItem                               usageMenuItem;          // != null if usage menu item available
  private JMenuItem[]                             toggleItems;            // optional togglenode menu items
  private JSeparator                              toggleSeparator;        // != null if sep. exists
  private JMenuItem[]                             extraItems;             // additional popup menu items from TreeExtension
  private JSeparator                              extraSeparator;         // != null if sep. exists
  private Collection                              objCollection;          // List of objects in tree
  private int                                     maxDepthForExtractPath; // default max. depth to show "extract path" button
  
  
  /**
   * Creates a tree.<br>
   * If the given object is a {@link Collection} the objects of the collection
   * will be shown in the tree. If it is some other object, only that
   * object is shown.<br>
   * Notice that the objects need not necessarily be {@link AppDbObject}s.
   * 
   * @param object the object or collection of objects, null if empty tree
   */
  @SuppressWarnings("unchecked")
  public AppDbObjectTree(Object object) {
    
    // setup the collection of objects or a single object or
    // an empty collection (null, see buildTree)
    Collection col = null;
    if (object instanceof Collection) {
      col = (Collection)object;
    }
    else if (object != null) {
      col = new ArrayList();
      col.add(object);
    }
    
    // don't use default DND! This would cause linux desktops to hang!
    setDragEnabled(false);
    
    maxDepthForExtractPath = 5;
    popupEnabled = true;
    addTreeWillExpandListener(this);
    
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        checkPopup(e, true);
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        checkPopup(e, true);
      }
    });
    
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e)  {
        int keyCode = e.getKeyCode();
        if (e.isControlDown())  {
          switch (keyCode) {
            case KeyEvent.VK_X:
            case KeyEvent.VK_Z:
            case KeyEvent.VK_N:
            case KeyEvent.VK_E:
            case KeyEvent.VK_O:
            case KeyEvent.VK_D:
            case KeyEvent.VK_C:
            case KeyEvent.VK_V:
            case KeyEvent.VK_U:
              checkPopup(e, false);   // determine items, but don't show menu
              if (keyCode == KeyEvent.VK_X && expandItem.isEnabled() && expandItem.isVisible())  {
                expandItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_Z && collapseItem.isEnabled() && collapseItem.isVisible())  {
                collapseItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_N && showItem.isEnabled() && showItem.isVisible())  {
                showItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_E && editItem.isEnabled() && editItem.isVisible())  {
                editItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_O && openItem.isEnabled() && openItem.isVisible())  {
                openItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_D && deleteItem.isEnabled() && deleteItem.isVisible())  {
                deleteItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_C && copyItem.isEnabled() && copyItem.isVisible())  {
                copyItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_V && insertItem.isEnabled() && insertItem.isVisible())  {
                insertItem.doClick();
              }
              else if (keyCode == KeyEvent.VK_H && historyItem.isEnabled() && historyItem.isVisible())  {
                historyItem.doClick();
              }
          }
        }
        else if (keyCode == KeyEvent.VK_SPACE)  {
          checkPopup(e, true);
        }
      }
    });
    
  
    /**
     * override transferhandler 
     */
    setTransferHandler(new TransferHandler() {
      
      private static final long serialVersionUID = 6532199703856836746L;
      
      @Override
      public int getSourceActions(JComponent c) {
        return COPY;
      }
      
      @Override
      protected Transferable createTransferable(JComponent c) {
        TreePath[] paths = getSelectionPaths();
        if (paths != null)  {
          List<AppDbObject> objectList = new ArrayList<AppDbObject>();
          for (int i=0; i < paths.length; i++)  {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)(paths[i].getLastPathComponent());
            Object obj = ((AppDbTreeObject)node.getUserObject()).getObject();
            if (obj instanceof AppDbObject) {
              objectList.add((AppDbObject)obj);
            }
          }
          return new AppDbObjectTransferable(objectList);
        }
        // else: default implementation
        return super.createTransferable(c);
      }
    });
    
    
    initComponents();

    // setup cell renderers
    setCellRenderer(createDefaultRenderer());
      
    // default to single selection
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    // create drag source
    DragSource dnd = DragSource.getDefaultDragSource();
    dnd.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    
    // create drop target
    DropTarget target = new DropTarget (this, this);
    target.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

    // build the tree
    buildTree(col);   

    // show tooltips of renderers
    // "unregister" not necessary cause registerComponent alters only "this", 
    // no references in the ToolTipManager!
    ToolTipManager.sharedInstance().registerComponent(this);
  }
  
  /**
   * Creates an empty tree.
   */
  public AppDbObjectTree()  {
    this(null);
  }

  
  /**
   * Creates the default tree cell renderer.<br>
   * The default implementation returns a {@link AppDbObjectTreeCellRenderer}.
   * 
   * @return the renderer
   */
  public TreeCellRenderer createDefaultRenderer() {
    return new AppDbObjectTreeCellRenderer();
  }
  
  
  /**
   * Enables/disables the popup-menu for nodes.
   * 
   * @param enabled true to enable popup menus (default)
   */
  public void setPopupEnabled(boolean enabled)  {
    popupEnabled = enabled;
  }

  /**
   * Returns whether popup-menu for nodes are enabled.
   * 
   * @return true if popup menus are enabled (default)
   */
  public boolean isPopupEnabled() {
    return popupEnabled;
  }

  
  /**
   * Sets the maximum treepath depth up to which the "extract path"-button
   * is displayed in the popupmenu. Default is 5.
   *
   * @param maxDepth the maximum path depth
   */
  public void setMaxDepthForExtractPath(int maxDepth) {
    this.maxDepthForExtractPath = maxDepth;
  }
  
  /**
   * Gets the maximum treepath depth up to which the "extract path"-button
   * is displayed in the popupmenu.
   *
   * @return the maximum path depth
   */
  public int getMaxDepthForExtractPath() {
    return maxDepthForExtractPath;
  }
  

  /**
   * Shows the popup menu at current selection.
   */
  public void showPopup() {
    checkPopup(null, true);
  }
  

  /**
   * Builds the tree from a collection of objects.
   * 
   * @param col the collection, null to set the empty collection
   */
  public void buildTree (Collection col)  {

    this.objCollection = col == null ? new ArrayList() : col;
    
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    if (col != null) {
      for (Object obj: col) {
        if (obj != null)  {
          if (obj instanceof AppDbObject) {
            obj = ((AppDbObject)obj).getTreeRoot();    // replace by root-object, if not the same
          }
          DefaultMutableTreeNode node = new DefaultMutableTreeNode (new AppDbTreeObject(obj, null));
          node.setAllowsChildren(
            (obj instanceof AppDbObjectTreeExtension && ((AppDbObjectTreeExtension)obj).allowsTreeChildObjects()) || 
            (obj instanceof AppDbObject && ((AppDbObject)obj).allowsTreeChildObjects()));
          root.add (node);
        }
      }
    }

    DefaultTreeModel tModel = new DefaultTreeModel (root);
    tModel.setAsksAllowsChildren(true);
    setModel (tModel);
    putClientProperty("JTree.lineStyle", "Angled");
  }


  /**
   * Expands all items in this tree.
   *
   * @param maxLevel is the maximum number of levels to expand, 0 = all
   */
  public void expandTree(int maxLevel)  {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(treeModel.getRoot());
    if (node != null) {
      doExpandPath(0, maxLevel, null, new TreePath(node));
    }
  }
  
  /**
   * Expands all items in this tree, unlimited.
   */
  public void expandTree()  {
    expandTree(0); 
  }
  
  
  /**
   * Transfer the focus to the first item in tree.
   */
  public void requestFocusForFirstItem() {
    setSelectionRow(0);
    scrollRowToVisible(0);
    FormHelper.requestFocusLater(this);
  }

  
  /**
   * Checks whether the given object is appendable.<br>
   * An object is appendable if it is not null, not an AppDbObject
   * or an AppDbObject with granted read permission.
   * 
   * @param childObject the object to append
   * @return true if appendable
   */
  public boolean isObjectAppendable(Object childObject)  {
    return childObject != null &&                       // don't append null objects
           (!(childObject instanceof AppDbObject) ||    // append if not an AppDbObject
            // if an AppDbObject: append if read-permission is not denied
            !((AppDbObject)childObject).isPermissionDenied(Security.READ));
  }
  
  
  /**
   * Checks whether given object is in path (parents to root) or not.<br>
   * Used to detect recursion loops.
   * 
   * @param childObject the object to check
   * @param node the node to start the search up to the root
   * @return true if object is in already in path
   */
  public boolean isObjectInParents(Object childObject, DefaultMutableTreeNode node)  {
    while (node != null) {
      // check if childobject is already part of the path (avoids recursion and confusion of user)
      Object pathObject = node.getUserObject();
      if (pathObject instanceof AppDbTreeObject && 
          ((AppDbTreeObject)pathObject).getObject() != null &&
          ((AppDbTreeObject)pathObject).getObject().equals(childObject))  {
        return true; // already in this path
      }
      node = (DefaultMutableTreeNode)node.getParent();
    }
    return false;
  }
  

  /**
   * Checks whether given object is in some of the child paths down to the leafs.<br>
   * Used to detect recursion loops.
   * 
   * @param childObject the object to check
   * @param node the node to start the search up to the root
   * @return true if object is in already in path
   */
  public boolean isObjectInChilds(Object childObject, DefaultMutableTreeNode node)  {
    if (childObject != null && node != null)  {
      Object pathObject = node.getUserObject();
      if (pathObject instanceof AppDbTreeObject && 
          ((AppDbTreeObject)pathObject).getObject() != null &&
          ((AppDbTreeObject)pathObject).getObject().equals(childObject))  {
        return true;
      }
      Enumeration ce = node.children();
      while (ce.hasMoreElements())  {
        if (isObjectInChilds(childObject, ((DefaultMutableTreeNode)ce.nextElement()))) {
          return true; 
        }
      }
    }
    return false;
  }
  

  /** 
   * Recursively expands the path.
   *
   * @param level is the current tree level, 0 = top
   * @param maxLevel is the maximum level not to exceed, 0 = unlimited
   * @param stopObject stops expansion if object met, null = unlimited
   * @param path is the path to expand
   */
  public void doExpandPath(int level, int maxLevel, AppDbObject stopObject, TreePath path)  {
    
    // expand this path
    expandPath(path);
    
    if (maxLevel == 0 || level + 1 < maxLevel)  {
      // expand all other objects
      DefaultMutableTreeNode  node     = (DefaultMutableTreeNode)path.getLastPathComponent();
      Enumeration             children = node.children();

      while (children.hasMoreElements())  {
        node = (DefaultMutableTreeNode)(children.nextElement());
        if (node.getUserObject() instanceof AppDbTreeObject)  {
          AppDbTreeObject userObject = (AppDbTreeObject)node.getUserObject();
          if ((level > 0 && userObject.isStopExpandPath()) ||
              (stopObject != null && userObject.getObject().equals(stopObject))) {
            continue;
          }
        }
        // add node, it's new
        if (node.getAllowsChildren()) {
          // recursively expand
          doExpandPath(level + 1, maxLevel, stopObject, new TreePath(node.getPath()));
        }
      }
    }
  }


  /**
   * Collapses a given path.<br>
   * The method invokes {@code collapsePath} and set {@code expanded=false}
   * in all {@link AppDbTreeObject}-nodes. Furthermore, all nodes
   * referring to {@link AppDbTreeToggleNodeObject}s will get their childs removed.
   * Tentackle applications should not use {@code collapsePath} directly.
   * 
   * @param path the tree path to collapse
   * @see javax.swing.JTree#collapsePath(javax.swing.tree.TreePath) 
   */
  public void doCollapsePath(TreePath path)  {
    collapsePath(path);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(path.getLastPathComponent());
    AppDbTreeObject tobj = (AppDbTreeObject)(node.getUserObject());
    tobj.setExpanded(false);
    if (!(tobj instanceof AppDbTreeToggleNodeObject))  {
      node.removeAllChildren();
    }
    ((DefaultTreeModel)treeModel).reload(node);
  }
  
  
  /**
   * Checks whether a path contains only {@link AppDbObject}s.
   * 
   * @param path the tree path
   * @return true if only {@code AppDbObject}s.
   */
  public boolean pathConsistsOfAppDbObjects(TreePath path) {
    int depth = path.getPathCount();
    for (int i = 1; i < depth; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getPathComponent(i);
      if ((((AppDbTreeObject)node.getUserObject()).getObject() instanceof AppDbObject) == false) {
        return false;
      }
    }
    return true;
  }
  
  


  
  // ------------- implements TreeWillExpandListener --------------------------
  
  /**
   * {@inheritDoc}
   * <p>
   * Loads child objects from the database.
   * @param e the expansion event
   * @throws javax.swing.tree.ExpandVetoException
   */
  public void treeWillExpand (TreeExpansionEvent e) throws ExpandVetoException {

    // get node which will be expanded
    TreePath path = e.getPath();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(path.getLastPathComponent());
    
    if (node != null) {

      AppDbTreeObject tobj = (AppDbTreeObject)(node.getUserObject());

      if (tobj != null && tobj.isExpanded() == false) {
        
        if (tobj.isStopTreeWillExpand()) {
          tobj.setStopTreeWillExpand(false);
          throw new ExpandVetoException(e);
        }
        
        // load child objects
        List<Object> childList = null;
        
        Object object = tobj.getObject();
        
        FormHelper.setWaitCursor(this);
        
        if (object instanceof AppDbObjectTreeExtension)  {
          childList = ((AppDbObjectTreeExtension)object).getTreeChildObjects(tobj.getParentObject()); 
        }
        
        if (object instanceof AppDbObject)  {
          childList = ((AppDbObject)object).getTreeChildObjects(tobj.getParentObject()); 
        }
        
        if (childList != null)  {
          for (Object obj: childList)  {
            if (isObjectAppendable(obj))  {
              AppDbTreeObject to = new AppDbTreeObject(obj, object);
              to.setStopTreeWillExpand(isObjectInParents(obj, node));
              DefaultMutableTreeNode childnode = new DefaultMutableTreeNode(to);
              childnode.setAllowsChildren(
                (obj instanceof AppDbObjectTreeExtension && ((AppDbObjectTreeExtension)obj).allowsTreeChildObjects()) ||
                (obj instanceof AppDbObject && ((AppDbObject)obj).allowsTreeChildObjects()));
              ((DefaultTreeModel)treeModel).insertNodeInto(childnode, node, node.getChildCount());
            }
          }
        }
        
        FormHelper.setDefaultCursor(this);

        // mark it expanded
        tobj.setExpanded(true);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing.
   * Applications may override.
   * 
   * @param e the expansion event
   */
  public void treeWillCollapse (TreeExpansionEvent e) {}



  // ------------------ implements DragGestureListener interface ---------------------
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation creates the transferable and starts the drag
   * if the node refers to an {@link AppDbObject} or {@link AppDbObjectTreeExtension}.
   * 
   * @param event the gesture event
   */
  public void dragGestureRecognized (DragGestureEvent event) {
    dragObject = null;
    dragNode   = null;
    TreePath path = getSelectionPath();
    if (path != null)  {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)(path.getLastPathComponent());
      Object obj = ((AppDbTreeObject)node.getUserObject()).getObject();
      Transferable tr = null;
      if (obj instanceof AppDbObjectTreeExtension) {
        tr = ((AppDbObjectTreeExtension)obj).getTransferable();
      }
      if (tr == null && obj instanceof AppDbObject) {
        dragNode   = node;
        dragObject = (AppDbObject)obj;
        tr         = dragObject.getTransferable();
      }
      // start the drag
      if (tr instanceof FileTransferable) {
        event.startDrag(event.getDragAction() == DnDConstants.ACTION_MOVE ?
                            DragSource.DefaultMoveDrop : DragSource.DefaultCopyDrop, tr, this);
      }
      else if (tr != null)  {
        event.startDrag (null, tr, this);
      }
    }
  }
  


  // ------------- implements DragSourceListener interface ---------------
  
  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param event the drag source event
   */
  public void dragEnter (DragSourceDragEvent event) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param event the drag source event
   */
  public void dragOver (DragSourceDragEvent event)  {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param event the drag source event
   */
  public void dragExit (DragSourceEvent event)  {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param event the drag source event
   */
  public void dropActionChanged (DragSourceDragEvent event) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param event the drag source event
   */
  public void dragDropEnd (DragSourceDropEvent event) {
  }

  

  // ----------------------- implements the DropTargetListener: -----------------------

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param dtde the drop target drag event
   */
  public void dragEnter(DropTargetDragEvent dtde) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to check whether to accept the drag or not.
   * 
   * @param dtde the drop target drag event
   */
  public void dragOver(DropTargetDragEvent dtde) {
    Point p = dtde.getLocation();
    TreePath path = getPathForLocation(p.x, p.y);
    if (path != null)  {
      DefaultMutableTreeNode node  = (DefaultMutableTreeNode)(path.getLastPathComponent());
      AppDbTreeObject mto = (AppDbTreeObject)(node.getUserObject());
      Object obj = mto.getObject();
      if (obj instanceof AppDbObject)  {
        Object transData = ((AppDbObject)obj).getTransientData();
        if (((AppDbObject)obj).isNew() == false)  {
          obj = ((AppDbObject)obj).reload();   // reload it to check whether deleted in the meantime
        }
        if (obj != null)  {
          ((AppDbObject)obj).setTransientData(transData);
          mto.setObject(obj);   // update in case changed
          if (((AppDbObject)obj).allowsTreeChildObjects())  {
            setSelectionPath(path);
            popupPath = path;
            popupObject = (AppDbObject)obj;
            popupNode = node;
            dtde.acceptDrag (dtde.getDropAction());
            return;
          }
        }
      }
    }
    // nicht akzeptieren!
    clearSelection();
    popupPath = null;
    popupObject = null;
    popupNode = null;
    dtde.rejectDrag();
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param dtde the drop target drag event
   */
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default implementation does nothing. Provided to be overridden.
   * @param dte the drop target event
   */
  public void dragExit(DropTargetEvent dte) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to insert the object.
   * @param dtde the drop target drop event
   * @see AppDbObject#dropTransferable(java.awt.datatransfer.Transferable) 
   */
  public void drop(DropTargetDropEvent dtde) {
    try {
      Transferable tr = dtde.getTransferable();
      insertDndOrCb(tr, dtde);
    }
    catch (Exception e) {
      AppworxGlobal.logger.severe(e.toString());
    }
  }

  
  

  
  /**
   * Hides the "in use" tree.
   * 
   * @param the child index of the node to be remove from the parent
   * @see AppDbObjectTreeExtensionUsageToggleNode
   */
  void hideInUseTree(int childIndex) {
    ((DefaultTreeModel)treeModel).removeNodeFromParent((DefaultMutableTreeNode)popupNode.getChildAt(childIndex));
  }
  
  /**
   * Shows the "in use" tree.
   * (package scope!)
   * @see AppDbObjectTreeExtensionUsageToggleNode
   */
  void showInUseTree()  {
    
    FormHelper.setWaitCursor(this);
    
    // build the "root" parents, i.e. all unique parent with no more
    List<DefaultMutableTreeNode> rootNodes = new ArrayList<DefaultMutableTreeNode>();
    // TreeParentObjects.
    List<Object> parentList = popupObject.getTreeParentObjects(((AppDbTreeObject)popupNode.getUserObject()).getParentObject());
    if (parentList != null && parentList.size() > 0)  {
      for (Object obj: parentList) {
        if (obj instanceof AppDbObject) {
          // build new path reverse from object to root
          addTreeParentRootNodes((AppDbObject)obj, new DefaultMutableTreeNode(new AppDbTreeObject(popupObject, null)), rootNodes);
        }
      }
    }
    // add rootnodes
    if (rootNodes.size() > 0) {
      DefaultMutableTreeNode inUseNode = new DefaultMutableTreeNode(usageToggleNode.getToggleNodeObject(popupObject));
      for (DefaultMutableTreeNode node: rootNodes)  {
        Enumeration ce = inUseNode.children();
        while (ce.hasMoreElements())  {
          DefaultMutableTreeNode childnode = (DefaultMutableTreeNode)ce.nextElement();
          if (childnode.getUserObject().equals(node.getUserObject())) {
            // merge childs (can be only one!!!)
            if (node.getChildCount() > 0) {
              DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)node.getChildAt(0);
              childnode.add(firstChild);
              node = null;
              break;
            }
          }
        }
        if (node != null) {
          inUseNode.add(node);
        }
      }
      ((DefaultTreeModel)treeModel).insertNodeInto(inUseNode, popupNode, 0);
      doExpandPath(0, 1, null, popupPath);    // expand one level
      TreePath inUsePath = popupPath.pathByAddingChild(inUseNode);
      doExpandPath(0, 0, popupObject, inUsePath);   // expand inUseNode recursively up to popupObject
      Enumeration ce = inUseNode.children();
      while (ce.hasMoreElements())  {
        collapsePath(inUsePath.pathByAddingChild(ce.nextElement()));
      }
      
      FormHelper.setDefaultCursor(this);
    }
    
    else  {
      FormHelper.setDefaultCursor(this);
      FormInfo.print(Locales.bundle.getString("no_references_found")); 
    }
  }
  
  

  
  /**
   * Inserts a transferable.
   * Same for clipboard and DnD operations.
   */
  private boolean insertDndOrCb(Transferable tr, DropTargetDropEvent dtde) throws UnsupportedFlavorException, IOException {
    
    if (popupPath != null && popupNode != null &&
       // check if we can drop on that target, i.e. target is writable
       !popupObject.isPermissionDenied(Security.WRITE)) {

      if (dtde != null) {
        dtde.acceptDrop (dtde.getDropAction());
      }

      if (popupObject.dropTransferable(tr)) {
        // show changes
        if (((AppDbTreeObject)popupNode.getUserObject()).isExpanded())  {
          // already expanded: collapse first and then refresh
          doCollapsePath(popupPath);
        }
        doExpandPath(0, 1, null, popupPath);    // recursively expand one level
        if (dtde != null) {
          dtde.dropComplete(true);
        }
        return true;
      }
      else  {
        if (dtde != null) {
          dtde.dropComplete(false);
        }
      }
    }
    return false;
  }
  
  
  /**
   * Adds tree parents "subtrees" as nodes.
   * 
   * @param parentObj the parent object
   * @param childNode the node
   * @param rootNodes the parent subtrees to add
   */
  private void addTreeParentRootNodes(AppDbObject parentObj, DefaultMutableTreeNode childNode, List<DefaultMutableTreeNode> rootNodes) {
    // end of path is always current object
    while (isObjectAppendable(parentObj)) {
      AppDbTreeObject tobj = new AppDbTreeObject(parentObj, null);
      tobj.setExpanded(true);
      DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(tobj);
      parentNode.add(childNode);
      if (isObjectInChilds(parentObj, childNode) == false)  {
        List p = parentObj.getTreeParentObjects(tobj.getParentObject());
        if (p != null && p.size() > 0)  {
          if (p.size() > 1) {
            // more than one parent: continue for each on a copy of the path so far
            for (int i=1; i < p.size(); i++)  {
              Object obj = p.get(i);
              if (obj instanceof AppDbObject) {
                // duplicate path starting at childnode
                addTreeParentRootNodes((AppDbObject)obj, duplicateTreeNode(parentNode), rootNodes);
              }
            }
          }
          // continue with single/first parent node
          childNode = parentNode;
          Object obj = p.get(0);
          if (obj instanceof AppDbObject) {
            parentObj = (AppDbObject)obj;
            continue;
          }
        }
      }
      // root reached
      rootNodes.add(parentNode);
     
      break;
    }
  }


  /**
   * Checks whether to display the popup and displays it.
   * 
   * @param e the input event (key press, mouse, etc...)
   * @param show true if show, else check only (used to determine some local vars)
   */
  private void checkPopup(InputEvent e, boolean show) {

    if (popupEnabled) {   // usually right mouse button pressed

      Point p = null;
      
      if (e instanceof MouseEvent)  {
        if(((MouseEvent)e).isPopupTrigger() == false) {
          return; // do nothing
        }
        p = ((MouseEvent)e).getPoint();
        popupPath = getPathForLocation(p.x, p.y);
      }
      else {  // KeyEvent
        popupPath = this.getSelectionPath();
        if (popupPath != null)  {
          Rectangle r = this.getPathBounds(popupPath);
          if (r != null)  {
            p = new Point(r.x + r.width/2, r.y + r.height/2);
          }
          else  {
            popupPath = null;
          }
        }
      }

      if (show) {
        // remove old toggle- and extra items if any
        if (usageMenuItem != null)  {
          popupMenu.remove(usageMenuItem);
          usageMenuItem = null;
        }
        if (toggleItems != null) {
          for (int i=0; i < toggleItems.length; i++) {
            popupMenu.remove(toggleItems[i]); 
          }
          toggleItems = null;
        }
        if (toggleSeparator != null) {
          popupMenu.remove(toggleSeparator);
          toggleSeparator = null;
        }
        if (extraItems != null) {
          for (int i=0; i < extraItems.length; i++) {
            popupMenu.remove(extraItems[i]); 
          }
          extraItems = null;
        }
        if (extraSeparator != null) {
          popupMenu.remove(extraSeparator);
          extraSeparator = null;
        }
      }
      
      if (popupPath != null && p != null)  {

        popupNode  = (DefaultMutableTreeNode)(popupPath.getLastPathComponent());
        AppDbTreeObject mto = (AppDbTreeObject)(popupNode.getUserObject());
        Object obj = mto.getObject();
        
        selectAllItem.setVisible(getSelectionModel().getSelectionMode() != ListSelectionModel.SINGLE_SELECTION);

        if (popupNode.getAllowsChildren())  {
          expandItem.setText(Locales.bundle.getString("expand"));
          if (mto.isExpanded()) {
            expandItem.setEnabled(true);
            expandItem.setText(Locales.bundle.getString("expand_again"));
            collapseItem.setEnabled(true);
          }
          else  {
            expandItem.setEnabled(true);
            collapseItem.setEnabled(false);
          }
        } else  {
          expandItem.setEnabled(false);
          collapseItem.setEnabled(false);
        }

        
        openEditor = null;
        // enable open?, extra items?
        if (obj instanceof AppDbObjectTreeExtension) {
          openEditor = ((AppDbObjectTreeExtension)obj).getOpenEditor();
          if (show) {
            extraItems = ((AppDbObjectTreeExtension)obj).getExtraMenuItems(this, popupNode);
            if (extraItems != null && extraItems.length > 0)  {
              extraSeparator = new JSeparator();
              popupMenu.add(extraSeparator);
              for (int i=0; i < extraItems.length; i++) {
                popupMenu.add(extraItems[i]); 
              }
            }
            
            AppDbObjectTreeExtensionToggleNode[] toggleNodes = ((AppDbObjectTreeExtension)obj).getToggleNodes(this, popupNode);
            if (toggleNodes != null)  {
              toggleItems = new JMenuItem[toggleNodes.length];
              for (int i=0; i < toggleNodes.length; i++) {
                toggleItems[i] = toggleNodes[i].getMenuItem(this, popupNode, popupPath);
              }
              if (toggleItems.length > 0)  {
                toggleSeparator = new JSeparator();
                popupMenu.add(toggleSeparator, 0);
                for (int i=0; i < toggleItems.length; i++) {
                  popupMenu.add(toggleItems[i], 0); 
                }
              }              
            }
          }
        }
        openItem.setVisible(openEditor != null);

        boolean writeable      = false;
        boolean instantiatable = true;
        
        subTreeItem.setVisible(false);
        makeTableItem.setVisible(false);
        extractPathItem.setVisible(false);
        
        if (obj instanceof AppDbObject) {
          
          Object transData = ((AppDbObject)obj).getTransientData();
          
          if (!FormHelper.isParentWindowModal(this))  {
            int depth = popupPath.getPathCount() - 1;
            subTreeItem.setVisible(depth > 1 || objCollection.size() > 1);
            if (depth > 1) {
              makeTableItem.setText(MessageFormat.format(Locales.bundle.getString("maketable"), ((AppDbObject)obj).getMultiName()));
              makeTableItem.setVisible(true);
              if (depth < maxDepthForExtractPath && pathConsistsOfAppDbObjects(popupPath)) {
                extractPathItem.setText(MessageFormat.format(Locales.bundle.getString("columntree"), ((AppDbObject)obj).getMultiName()));
                extractPathItem.setVisible(true);
              }
            }
          }
          
          if (((AppDbObject)obj).isNew())  {
            popupObject = (AppDbObject)obj;
            copyItem.setEnabled(false);
            insertItem.setEnabled(false);
          }
          else  {
            popupObject = (AppDbObject)((AppDbObject)obj).reload();   // reload for sure
            copyItem.setEnabled(true);
            insertItem.setEnabled(true);
          }
          
          mto.setObject(popupObject);
          
          if (popupObject == null) {
            return; // vanished (i.e. someone deleted the object in the meantime)
          }
          
          instantiatable = popupObject.isInstantiatable();
          popupObject.setTransientData(transData);
          obj = popupObject;
          
          if (popupObject.allowsTreeParentObjects())  {
            if (toggleSeparator == null) {
              toggleSeparator = new JSeparator();
              popupMenu.add(toggleSeparator, 0);              
            }
            usageToggleNode = new AppDbObjectTreeExtensionUsageToggleNode();
            usageMenuItem = usageToggleNode.getMenuItem(this, popupNode, popupPath);
            popupMenu.add(usageMenuItem, 0);
          }
          else  {
            usageToggleNode = null;
          }
          
          // check permission of object
          writeable = !popupObject.isPermissionDenied(Security.WRITE);
          
          historyItem.setVisible(writeable && popupObject.allowsHistory());

          if (popupObject.panelExists() &&
              !popupObject.isPermissionDenied(Security.READ))  {
            editItem.setVisible(popupObject.isEditableLazy() && writeable);
            showItem.setVisible(true);
          }
          else  {
            editItem.setVisible(false);
            showItem.setVisible(false);
          }
          
          // refresh display (bec. of reload())
          ((DefaultTreeModel)treeModel).nodeChanged(popupNode);
          // Objekt erneut selektieren
          setSelectionPath(popupPath);
        }
        
        else  {
          historyItem.setVisible(false);
          copyItem.setEnabled(false);
          insertItem.setEnabled(false);
          editItem.setVisible(false);
          showItem.setVisible(false);
        }
        
        // enable delete?
        boolean deleteable = writeable;
        if (deleteable) {
          if (obj instanceof AppDbObject && ((AppDbObject)obj).isRemovableLazy() == false ||
              obj instanceof AppDbObjectTreeExtension && ((AppDbObjectTreeExtension)obj).isRemovable() == false) {
            deleteable = false;
          }
        }
        deleteItem.setEnabled(deleteable);
        deleteItem.setVisible(instantiatable);
          
        // show popup menu
        if (show) {
          popupMenu.show(this, p.x, p.y);
        }
      }
    }
  }

  
  /**
   * Duplicates a node and all its childnodes.
   */
  private DefaultMutableTreeNode duplicateTreeNode(DefaultMutableTreeNode node) {
    DefaultMutableTreeNode parent = new DefaultMutableTreeNode(node.getUserObject(), node.getAllowsChildren());
    for (int i=0; i < node.getChildCount(); i++)  {
      parent.add(duplicateTreeNode((DefaultMutableTreeNode)node.getChildAt(i))); 
    }
    return parent;
  }
  


  /**
   * Finds the treenode for an object which is part of the displayed collection.<br>
   * 
   * @param object the object 
   * @return the child of the root node if found, null if no such node
   */
  public DefaultMutableTreeNode findNodeInCollection(Object object) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    // find object in childnodes
    Enumeration childs = root.children();
    while (childs.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) childs.nextElement();
      if (node.getUserObject() instanceof AppDbTreeObject &&
          ((AppDbTreeObject)node.getUserObject()).getObject().equals(object)) {
        return node;
      }
    }
    return null;
  }



  /**
   * Find the treepath for an object which is part of the displayed collection.<br>
   * @param object the object
   * @return the path to the child of the root node if found, null if no such node
   */
  public TreePath findPathInCollection(Object object) {
    DefaultMutableTreeNode node = findNodeInCollection(object);
    if (node != null) {
      return new TreePath(new Object[] { getModel().getRoot(), node });
    }
    return null;
  }


  /**
   * Collapses all childnodes of the root.
   */
  public void collapseAll() {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    // find object in childnodes
    Enumeration childs = root.children();
    while (childs.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) childs.nextElement();
      collapsePath(new TreePath(new Object[] { getModel().getRoot(), node }));
    }
    TreePath path = getSelectionPath();   // gets the first selection, if any
    if (path != null) {
      scrollPathToVisible(path);
    }
  }

  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    popupMenu = new javax.swing.JPopupMenu();
    historyItem = new javax.swing.JMenuItem();
    expandItem = new javax.swing.JMenuItem();
    collapseItem = new javax.swing.JMenuItem();
    subTreeItem = new javax.swing.JMenuItem();
    makeTableItem = new javax.swing.JMenuItem();
    extractPathItem = new javax.swing.JMenuItem();
    showItem = new javax.swing.JMenuItem();
    editItem = new javax.swing.JMenuItem();
    openItem = new javax.swing.JMenuItem();
    deleteItem = new javax.swing.JMenuItem();
    copyItem = new javax.swing.JMenuItem();
    selectAllItem = new javax.swing.JMenuItem();
    insertItem = new javax.swing.JMenuItem();

    historyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
    historyItem.setText(Locales.bundle.getString("History")); // NOI18N
    historyItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        historyItemActionPerformed(evt);
      }
    });

    popupMenu.add(historyItem);

    expandItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
    expandItem.setText(Locales.bundle.getString("expand")); // NOI18N
    expandItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        expandItemActionPerformed(evt);
      }
    });

    popupMenu.add(expandItem);

    collapseItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
    collapseItem.setText(Locales.bundle.getString("collapse")); // NOI18N
    collapseItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        collapseItemActionPerformed(evt);
      }
    });

    popupMenu.add(collapseItem);

    subTreeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    subTreeItem.setText(Locales.bundle.getString("subtree")); // NOI18N
    subTreeItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        subTreeItemActionPerformed(evt);
      }
    });

    popupMenu.add(subTreeItem);

    makeTableItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
    makeTableItem.setText("create table");
    makeTableItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        makeTableItemActionPerformed(evt);
      }
    });

    popupMenu.add(makeTableItem);

    extractPathItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
    extractPathItem.setText("extract path");
    extractPathItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        extractPathItemActionPerformed(evt);
      }
    });

    popupMenu.add(extractPathItem);

    showItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
    showItem.setText(Locales.bundle.getString("view")); // NOI18N
    showItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showItemActionPerformed(evt);
      }
    });

    popupMenu.add(showItem);

    editItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
    editItem.setText(Locales.bundle.getString("edit")); // NOI18N
    editItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        editItemActionPerformed(evt);
      }
    });

    popupMenu.add(editItem);

    openItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
    openItem.setText(Locales.bundle.getString("open")); // NOI18N
    openItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        openItemActionPerformed(evt);
      }
    });

    popupMenu.add(openItem);

    deleteItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
    deleteItem.setText(Locales.bundle.getString("delete")); // NOI18N
    deleteItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        deleteItemActionPerformed(evt);
      }
    });

    popupMenu.add(deleteItem);

    copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
    copyItem.setText(Locales.bundle.getString("copy")); // NOI18N
    copyItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        copyItemActionPerformed(evt);
      }
    });

    popupMenu.add(copyItem);

    selectAllItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
    selectAllItem.setText(Locales.bundle.getString("select_all")); // NOI18N
    selectAllItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        selectAllItemActionPerformed(evt);
      }
    });

    popupMenu.add(selectAllItem);

    insertItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
    insertItem.setText(Locales.bundle.getString("insert")); // NOI18N
    insertItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        insertItemActionPerformed(evt);
      }
    });

    popupMenu.add(insertItem);

    setRootVisible(false);
    setShowsRootHandles(true);
  }// </editor-fold>//GEN-END:initComponents

  private void makeTableItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makeTableItemActionPerformed
    // create a table from childobjects of given type
    List<AppDbObject> list = new ArrayList<AppDbObject>();
    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)popupPath.getParentPath().getLastPathComponent();
    int childNum = parentNode.getChildCount();
    for (int childIndex=0; childIndex < childNum; childIndex++) {
      DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)parentNode.getChildAt(childIndex);
      AppDbTreeObject to = (AppDbTreeObject)childNode.getUserObject();
      if (to.getObject().getClass() == popupObject.getClass()) {
        list.add((AppDbObject)to.getObject());
      }
    }
    if (list.isEmpty() == false) {
      AppDbObjectNaviDialog d = new AppDbObjectNaviDialog(list, null, true);
      d.setTitle(popupObject.getMultiName());
      d.getNaviPanel().getNaviTable().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      d.setVisible(true);
    }
  }//GEN-LAST:event_makeTableItemActionPerformed

  private void extractPathItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extractPathItemActionPerformed
    AppDbObject dbObject = popupObject;
    // get the tree class path
    int offset = 1;
    int depth = popupPath.getPathCount() - offset;   // we start at level 0
    Class clazz[] = new Class[depth];
    for (depth=0; depth < clazz.length; depth++)  {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)popupPath.getPathComponent(depth + offset);
      AppDbTreeObject mto = (AppDbTreeObject)node.getUserObject();
      clazz[depth] = mto.getObject().getClass();
    }
    // notice: we can't execute in the modthread on its own db because
    // we don't know how relations are cached within the treepath.
    // Therefore we cannot alter the Db afterwards.
    FormHelper.setWaitCursor(this);
    List<AppDbObject> objList = AppDbObject.extractTreePath(objCollection, clazz); // this may take a lot of time!
    if (objList != null && objList.isEmpty() == false) {
      AppDbObjectNaviDialog d = new AppDbObjectNaviDialog(objList, null);
      d.setTitle(dbObject.getMultiName());
      d.getNaviPanel().getNaviTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      d.setVisible(true);
    }
    FormHelper.setDefaultCursor(this);
  }//GEN-LAST:event_extractPathItemActionPerformed

  private void subTreeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTreeItemActionPerformed
    AppDbObject dbObject = popupObject;
    AppDbObjectNaviDialog d = new AppDbObjectNaviDialog(dbObject, null);
    d.setTitle(dbObject.getSingleName() + " " + dbObject);
    d.getNaviPanel().getNaviTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    d.getNaviPanel().getNaviTree().expandTree(2);
    d.setVisible(true);
  }//GEN-LAST:event_subTreeItemActionPerformed

  private void historyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_historyItemActionPerformed
    new HistoryTableDialog(false).showDialog(popupObject);
  }//GEN-LAST:event_historyItemActionPerformed

  private void insertItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertItemActionPerformed
    try {
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable trans = clip.getContents(this);
      insertDndOrCb(trans, null); 
    }
    catch (Exception e) {
      AppworxGlobal.logger.warning(e.getMessage());
      FormInfo.print(Locales.bundle.getString("couldn't_insert"));
    }
  }//GEN-LAST:event_insertItemActionPerformed

  private void selectAllItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllItemActionPerformed
    this.setSelectionInterval(0, getRowCount());
  }//GEN-LAST:event_selectAllItemActionPerformed

  private void copyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyItemActionPerformed
    try {
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      AppDbObjectTransferable trans = new AppDbObjectTransferable(popupObject);
      clip.setContents(trans, trans);
    }
    catch (Exception e) {
      // nothing to do
    }    
  }//GEN-LAST:event_copyItemActionPerformed

  private void openItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openItemActionPerformed
    if (openEditor != null) openEditor.run();
  }//GEN-LAST:event_openItemActionPerformed

  private void deleteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteItemActionPerformed
    if (Hook.hook().delete(popupObject)) {
      ((DefaultTreeModel)treeModel).removeNodeFromParent(popupNode);
    }
  }//GEN-LAST:event_deleteItemActionPerformed

  private void editItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editItemActionPerformed

    AppDbTreeObject treeObject = (AppDbTreeObject)popupNode.getUserObject();

    if (popupObject instanceof AppDbObjectTreeExtension)  {
      AppDbObjectTreeExtensionEditor editor = ((AppDbObjectTreeExtension)popupObject).getEditor();
      if (editor != null) {
        // edit object modal
        editor.showEditor(this, popupNode, true, false);
        return;
      }
    }
    
    if (FormHelper.isParentWindowModal(this)) {
      // edit modal if parent is modal too
      if (Hook.hook().editModal(popupObject) == null) {
        // cancelled
        return;
      }
    }
    else  {
      // non-modal (with close on save or delete)
      final AppDbObjectDialog d = Hook.hook().getDialogPool().useNonModalDialog(popupObject, true, true);
      if (objCollection instanceof List && popupPath.getPathCount() == 2) {
        d.setObjectList((List)objCollection);
      }

      collapseAll();

      d.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (e.getActionCommand() == AppDbObjectDialog.ACTION_SAVE) {
            AppDbObject obj = d.getLastObject();
            // reload to get the real data stored in DB (in case of CANCEL)
            if (!obj.isNew()) {
              obj = (AppDbObject)obj.reload();
            }
            if (obj != null)  {
              DefaultMutableTreeNode node = findNodeInCollection(obj);
              if (node != null) {
                ((AppDbTreeObject)node.getUserObject()).setObject(obj);
                ((DefaultTreeModel)treeModel).nodeChanged(node);  // refresh display
              }
            }
          }
          else if (e.getActionCommand() == AppDbObjectDialog.ACTION_DELETE) {
            AppDbObject obj = d.getLastObject();
            if (obj != null)  {
              DefaultMutableTreeNode node = findNodeInCollection(obj);
              if (node != null) {
                ((DefaultTreeModel)treeModel).removeNodeFromParent(node);  // remove node
              }
            }
          }
          else if (e.getActionCommand() == AppDbObjectDialog.ACTION_PREVIOUS ||
                   e.getActionCommand() == AppDbObjectDialog.ACTION_NEXT) {
            TreePath path = findPathInCollection(d.getObject());
            if (path != null) {
              scrollPathToVisible(path);
              setSelectionPath(path);
            }
          }
        }
      });
      return;
    }

    if (treeObject.isExpanded()) {
      doCollapsePath(popupPath);
      doExpandPath(0, 1, null, popupPath);
    }
    
    // refresh display
    ((DefaultTreeModel)treeModel).nodeChanged(popupNode);  
  }//GEN-LAST:event_editItemActionPerformed

  private void showItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showItemActionPerformed
    // show objects only, always non-modal
    if (popupObject instanceof AppDbObjectTreeExtension)  {
      AppDbObjectTreeExtensionEditor editor = ((AppDbObjectTreeExtension)popupObject).getEditor();
      if (editor != null) {
        // call it non-modal
        editor.showEditor(this, popupNode, false, true);
        return;
      }
    }
    if (FormHelper.isParentWindowModal(this)) {
      Hook.hook().viewModal(popupObject, this);
    }
    else  {
      // non-modal
      final AppDbObjectDialog d = Hook.hook().getDialogPool().useNonModalDialog(popupObject, false, false);
      if (objCollection instanceof List && popupPath.getPathCount() == 2) {
        d.setObjectList((List)objCollection);
      }

      collapseAll();

      d.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (e.getActionCommand() == AppDbObjectDialog.ACTION_PREVIOUS ||
                   e.getActionCommand() == AppDbObjectDialog.ACTION_NEXT) {
            TreePath path = findPathInCollection(d.getObject());
            if (path != null) {
              scrollPathToVisible(path);
              setSelectionPath(path);
            }
          }
        }
      });
    }
  }//GEN-LAST:event_showItemActionPerformed

  private void collapseItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_collapseItemActionPerformed
    doCollapsePath(popupPath);
  }//GEN-LAST:event_collapseItemActionPerformed

  private void expandItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandItemActionPerformed
    if (((AppDbTreeObject)popupNode.getUserObject()).isExpanded())  {
      // already expanded: collapse first and then refresh
      doCollapsePath(popupPath);
    }
    doExpandPath(0, popupObject.getTreeExpandMaxDepth(), null, popupPath);    // recursively expand all
  }//GEN-LAST:event_expandItemActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenuItem collapseItem;
  private javax.swing.JMenuItem copyItem;
  private javax.swing.JMenuItem deleteItem;
  private javax.swing.JMenuItem editItem;
  private javax.swing.JMenuItem expandItem;
  private javax.swing.JMenuItem extractPathItem;
  private javax.swing.JMenuItem historyItem;
  private javax.swing.JMenuItem insertItem;
  private javax.swing.JMenuItem makeTableItem;
  private javax.swing.JMenuItem openItem;
  private javax.swing.JPopupMenu popupMenu;
  private javax.swing.JMenuItem selectAllItem;
  private javax.swing.JMenuItem showItem;
  private javax.swing.JMenuItem subTreeItem;
  // End of variables declaration//GEN-END:variables
  
}

