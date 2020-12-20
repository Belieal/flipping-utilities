package com.flippingutilities.ui.uiutilities;

import com.flippingutilities.FlippingPlugin;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final Dimension ICON_SIZE = new Dimension(32, 32);
    public static final int TOOLBAR_BUTTON_SIZE = 20;

    public static final ImageIcon OPEN_ICON;
    public static final ImageIcon CLOSE_ICON;

    public static final ImageIcon RESET_ICON;
    public static final ImageIcon RESET_HOVER_ICON;

    public static final ImageIcon DELETE_ICON;

    public static final ImageIcon SETTINGS_ICON;
    public static final ImageIcon SETTINGS_ICON_OFF;

    public static final ImageIcon ACCOUNT_ICON;

    public static final ImageIcon DELETE_BUTTON;

    public static final ImageIcon HIGHLIGHT_DELETE_BUTTON;

    public static final ImageIcon STAR_ON_ICON;
    public static final ImageIcon STAR_HALF_ON_ICON;
    public static final ImageIcon STAR_OFF_ICON;


    public static final ImageIcon SORT_BY_RECENT_OFF_ICON;
    public static final ImageIcon SORT_BY_RECENT_ON_ICON;
    public static final ImageIcon SORT_BY_RECENT_HALF_ON_ICON;

    public static final ImageIcon SORT_BY_ROI_OFF_ICON;
    public static final ImageIcon SORT_BY_ROI_ON_ICON;
    public static final ImageIcon SORT_BY_ROI_HALF_ON_ICON;

    public static final ImageIcon SORT_BY_PROFIT_OFF_ICON;
    public static final ImageIcon SORT_BY_PROFIT_ON_ICON;
    public static final ImageIcon SORT_BY_PROFIT_HALF_ON_ICON;

    public static final ImageIcon ARROW_LEFT;
    public static final ImageIcon ARROW_RIGHT;
    public static final ImageIcon ARROW_LEFT_HOVER;
    public static final ImageIcon ARROW_RIGHT_HOVER;

    public static final ImageIcon HEART_ICON;

    public static final ImageIcon TRASH_ICON;
    public static final ImageIcon TRASH_ICON_OFF;

    public static final ImageIcon DOWNLOAD_ICON;
    public static final ImageIcon DONWLOAD_ICON_OFF;

    static
    {
        final BufferedImage openIcon = ImageUtil
                .getResourceStreamFromClass(FlippingPlugin.class, "/small_open_arrow.png");
        CLOSE_ICON = new ImageIcon(openIcon);
        OPEN_ICON = new ImageIcon(ImageUtil.rotateImage(openIcon, Math.toRadians(90)));

        final BufferedImage resetIcon = ImageUtil
                .getResourceStreamFromClass(FlippingPlugin.class, "/reset.png");
        RESET_ICON = new ImageIcon(resetIcon);
        RESET_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(resetIcon, 0.53f));

        final BufferedImage deleteIcon = ImageUtil
                .getResourceStreamFromClass(FlippingPlugin.class, "/delete_icon.png");
        DELETE_ICON = new ImageIcon(deleteIcon);

        final BufferedImage settingsIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/settings_icon.png");
        SETTINGS_ICON = new ImageIcon(settingsIcon);
        SETTINGS_ICON_OFF = new ImageIcon(ImageUtil.alphaOffset(settingsIcon, 0.53f));

        final BufferedImage accountIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/gnome.png");
        ACCOUNT_ICON = new ImageIcon(accountIcon);

        final BufferedImage deleteButton = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/deleteButton.png");
        DELETE_BUTTON = new ImageIcon(deleteButton);

        final BufferedImage highlightDeleteButton = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/highlightDeleteButton.png");
        HIGHLIGHT_DELETE_BUTTON = new ImageIcon(highlightDeleteButton);


        final BufferedImage starOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/star-gold.png");
        final BufferedImage sortByRecentOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/clock-gold.png");
        final BufferedImage sortByRoiOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/roi-gold.png");
        final BufferedImage sortByProfitOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/profit-gold.png");
        final BufferedImage starOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/star_off_white.png");
        final BufferedImage sortByRecentOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/clock_white.png");
        final BufferedImage sortByRoiOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/thick_roi_white.png");
        final BufferedImage sortByProfitOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/potential_profit_white.png");

        STAR_ON_ICON = new ImageIcon(starOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_RECENT_ON_ICON = new ImageIcon(sortByRecentOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_ROI_ON_ICON = new ImageIcon(sortByRoiOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_PROFIT_ON_ICON = new ImageIcon(sortByProfitOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

        STAR_HALF_ON_ICON = new ImageIcon(starOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_PROFIT_HALF_ON_ICON = new ImageIcon(sortByProfitOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_RECENT_HALF_ON_ICON = new ImageIcon(sortByRecentOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_ROI_HALF_ON_ICON = new ImageIcon(sortByRoiOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

        STAR_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(starOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_RECENT_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByRecentOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_ROI_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByRoiOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
        SORT_BY_PROFIT_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByProfitOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

        final BufferedImage arrowLeft = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/left-arrow.png");
        ARROW_LEFT = new ImageIcon(ImageUtil.alphaOffset(arrowLeft,0.70f));

        final BufferedImage arrowRight = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/right-arrow.png");
        ARROW_RIGHT = new ImageIcon(ImageUtil.alphaOffset(arrowRight,0.70f));

        ARROW_LEFT_HOVER = new ImageIcon(arrowLeft);
        ARROW_RIGHT_HOVER = new ImageIcon(arrowRight);

        final BufferedImage heart = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/heart.png");
        HEART_ICON = new ImageIcon(heart);

        final BufferedImage trashIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/trash.png");
        TRASH_ICON = new ImageIcon(trashIcon);
        TRASH_ICON_OFF = new ImageIcon(ImageUtil.alphaOffset(trashIcon, 0.53f));

        final BufferedImage downloadIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/download.png");
        DOWNLOAD_ICON = new ImageIcon(downloadIcon);
        DONWLOAD_ICON_OFF = new ImageIcon(ImageUtil.alphaOffset(downloadIcon, 0.53f));
    }
}
