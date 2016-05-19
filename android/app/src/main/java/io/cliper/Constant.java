package io.cliper;

import android.os.Environment;

/**
 * Created by adv_zxy on 5/19/16.
 */
public class Constant {
    public static String registerUrl = "ws://104.207.144.233:4564/register";
    public static String loginUrl = "ws://104.207.144.233:4564/login";
    public static String postUrl = "ws://104.207.144.233:4564/post";
    public static String syncUrl = "ws://104.207.144.233:4564/sync";
    public static String logoutUrl = "ws://104.207.144.233:4564/logout";
    public static String pingUrl = "ws://104.207.144.233:4564/ping";

    public static final String sdcardPath = Environment.getExternalStorageDirectory().getPath();
    public static final String tokenFilePath = "/cliper.token";
    public static final String msgFilePath = "/cliper.msg";
    public static final String tokenFileAbsPath = sdcardPath + tokenFilePath;
    public static final String msgFileAbsPath = sdcardPath + msgFilePath;
}
