# AppWidgetProvider and Worker entry points are referenced by the Android framework.
-keep class com.simplebattery.app.widget.**WidgetProvider { *; }
-keep class com.simplebattery.app.widget.WidgetUpdateWorker { *; }
