package nxtdesktop;

import com.sun.javafx.scene.control.skin.ContextMenuContent;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Iterator;

/**
 * Enables only the cut/copy/paste context menu for edit fields and disables the link and window context menus
 *
 * Inspired by http://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine
 * Hopefully, in Java 9 there will be a more standard way to implement this.
 */
class WalletContextMenu implements EventHandler<ContextMenuEvent> {

    @Override
    public void handle(ContextMenuEvent event) {
        @SuppressWarnings("deprecation")
        final Iterator<Window> windows = Window.impl_getWindows(); // May not work in Java 9

        while (windows.hasNext()) {
            // access the context menu window
            final Window window = windows.next();
            if (window instanceof ContextMenu) {
                if (window.getScene() != null && window.getScene().getRoot() != null) {
                    Parent root = window.getScene().getRoot();
                    if (root.getChildrenUnmodifiable().size() > 0) {
                        Node popup = root.getChildrenUnmodifiable().get(0);
                        if (popup.lookup(".context-menu") != null) {
                            Node bridge = popup.lookup(".context-menu");
                            ContextMenuContent cmc = (ContextMenuContent) ((Parent) bridge).getChildrenUnmodifiable().get(0);
                            VBox itemsContainer = cmc.getItemsContainer();
                            ObservableList<Node> children = itemsContainer.getChildren();
                            if (children.size() == 3) {
                                Node n = children.get(0);
                                ContextMenuContent.MenuItemContainer item=(ContextMenuContent.MenuItemContainer)n;
                                if (item.getItem().getText().equals("Cut")) {
                                    return;
                                }
                            }
                            event.consume();
                            window.hide();
                            return;
                        }
                    }
                }
                return;
            }
        }
    }
}