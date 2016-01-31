# StickyFlagView
StickyFlagView is an unread message tag which can drag.<br><br>
![](https://github.com/weijia1991/StickyFlagView/blob/master/sticky.gif)
# Usage
* In XML:<br><br>
1.default flag view <br><br>
`<com.wj.sticky.StickyFlagView` <br>
`　　android:layout_width="40dp"` <br>
`　　android:layout_height="40dp"` <br>
`　　app:flagRadius="10dp" />` <br><br>
2.flag view with drawable resource　<br><br>
`<com.wj.sticky.StickyFlagView` <br>
`　　android:layout_width="wrap_content"` <br>
`　　android:layout_height="wrap_content"` <br>
`　　app:flagDrawable="@drawable/bubble"` <br>
`　　app:flagColor="#f74c31" />` <br><br>
* In code: <br><br>
`StickyFlagView sfv = new StickyFlagView(MainActivity.this);` <br>
`sfv.setFlagText("9");` <br>
`sfv.setFlagTextColor(Color.WHITE);` <br>
`sfv.setFlagTextSize(15);` <br>
`sfv.setFlagColor(Color.RED);` <br>
`sfv.setFlagRadius(10);` <br>
`sfv.setMinStickRadius(3);` <br>
`sfv.setMaxStickRadius(8);` <br>
`sfv.setMaxDistance(80);` <br>
