package com.epicnorth.zentimer;

import android.net.Uri;

/**
 * Created by Trevor on 7/5/2014.
 */
public class Segment {
    public int Ordinal;
    public String Name;
    public Uri MusicUri;
    public long Duration;
    public long Interval; // amount of time between indications. eg a chime every 5 minutes
    public AnnouncementType IntervalType;
    public boolean UseMusicDuration;
}

