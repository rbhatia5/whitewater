#include "player.c"

int main(int argc, char *argv[]) {
  CustomData data;
  GstStateChangeReturn ret;
  GstBus *bus;
   
  /* Initialize GTK */
  gtk_init (&argc, &argv);
   
  /* Initialize GStreamer */
  gst_init (&argc, &argv);
   
  /* Initialize our data structure */
  memset (&data, 0, sizeof (data));
  data.duration = GST_CLOCK_TIME_NONE;
  data.rate = 1; 
  /* Create the elements */
  data.playbin2 = gst_element_factory_make ("playbin2", "playbin2");
    
  if (!data.playbin2) {
    g_printerr ("Not all elements could be created.\n");
    return -1;
  }
   
  /* Set the URI to play */
  g_object_set (data.playbin2, "uri", "http://docs.gstreamer.com/media/sintel_trailer-480p.webm", NULL);
   
   /* Connect to interesting signals in playbin2 */
  g_signal_connect (G_OBJECT (data.playbin2), "video-tags-changed", (GCallback) tags_cb, &data);
  g_signal_connect (G_OBJECT (data.playbin2), "audio-tags-changed", (GCallback) tags_cb, &data);
  g_signal_connect (G_OBJECT (data.playbin2), "text-tags-changed", (GCallback) tags_cb, &data);
  /* Create the GUI */
  create_ui (&data);
   
  /* Instruct the bus to emit signals for each received message, and connect to the interesting signals */
  bus = gst_element_get_bus (data.playbin2);
  gst_bus_add_signal_watch (bus);
  g_signal_connect (G_OBJECT (bus), "message::error", (GCallback)error_cb, &data);
  g_signal_connect (G_OBJECT (bus), "message::eos", (GCallback)eos_cb, &data);
  g_signal_connect (G_OBJECT (bus), "message::state-changed", (GCallback)state_changed_cb, &data);
   gst_bus_set_sync_handler (bus, (GstBusSyncHandler) bus_sync_handler, NULL);
  g_signal_connect (G_OBJECT (bus), "message::application", (GCallback)application_cb, &data);
  gst_object_unref (bus);
   
  /* Start playing */
  ret = gst_element_set_state (data.playbin2, GST_STATE_PLAYING);
  if (ret == GST_STATE_CHANGE_FAILURE) {
    g_printerr ("Unable to set the pipeline to the playing state.\n");
    gst_object_unref (data.playbin2);
    return -1;
  }
   
  /* Register a function that GLib will call every second */
  g_timeout_add_seconds (1, (GSourceFunc)refresh_ui, &data);
   
  /* Start the GTK main loop. We will not regain control until gtk_main_quit is called. */
  gtk_main ();
   
  /* Free resources */
  gst_element_set_state (data.playbin2, GST_STATE_NULL);
  gst_object_unref (data.playbin2);
  return 0;
}
