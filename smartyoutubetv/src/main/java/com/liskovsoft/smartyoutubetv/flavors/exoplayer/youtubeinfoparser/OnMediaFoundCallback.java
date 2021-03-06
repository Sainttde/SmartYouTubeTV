package com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser;

import android.net.Uri;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.misc.YouTubeGenericInfo;

import java.io.InputStream;

public abstract class OnMediaFoundCallback {
    public void onDashMPDFound(InputStream mpdContent){}
    public void onLiveUrlFound(Uri hlsUrl){}
    public void onInfoFound(YouTubeGenericInfo info){}
}
