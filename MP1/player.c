#include <string.h>
   
#include <gtk/gtk.h>
#include <gst/gst.h>
#include <gst/interfaces/xoverlay.h>
   
#include <gdk/gdk.h>
#if defined (GDK_WINDOWING_X11)
#include <gdk/gdkx.h>
#elif defined (GDK_WINDOWING_WIN32)
#include <gdk/gdkwin32.h>
#elif defined (GDK_WINDOWING_QUARTZ)
#include <gdk/gdkquartz.h>
#endif
   
/* Structure to contain all our information, so we can pass it around */
typedef struct _CustomData {
  GstElement *playbin2;           /* Our one and only pipeline */
  GstElement *video_sink;         /* Storage for the video sink */ 
  GtkWidget *slider;              /* Slider widget to keep track of current position */
  GtkWidget *streams_list;        /* Text widget to display info about the streams */
  gulong slider_update_signal_id; /* Signal ID for the slider update signal */
  GtkWidget *video_window;		  /* Main window of the UI */
  GtkWindow *dialog_window;
  GstState state;                 /* Current state of the pipeline */
  gint64 duration;                /* Duration of the clip, in nanoseconds */
  gdouble rate;
} CustomData;

static void send_seek_event (CustomData *);
  
static guintptr video_window_xid = 0;

static gboolean refresh_ui (CustomData *data);
/* This function is called when the GUI toolkit creates the physical window that will hold the video.
 * At this point we can retrieve its handler (which has a different meaning depending on the windowing system)
 * and pass it to GStreamer through the XOverlay interface. */



static GstBusSyncReply
bus_sync_handler (GstBus * bus, GstMessage * message, gpointer user_data)
{
 // ignore anything but 'prepare-xwindow-id' element messages
 if (GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT)
   return GST_BUS_PASS;
 if (!gst_structure_has_name (message->structure, "prepare-xwindow-id"))
   return GST_BUS_PASS;

 if (video_window_xid != 0) {


   // GST_MESSAGE_SRC (message) will be the video sink element
  
   gst_x_overlay_set_xwindow_id (GST_X_OVERLAY (GST_MESSAGE_SRC (message)), video_window_xid);
 } else {
   g_warning ("Should have obtained video_window_xid by now!");
 }

 gst_message_unref (message);
 return GST_BUS_DROP;
}

static void realize_cb (GtkWidget *widget, CustomData *data) {
#if GTK_CHECK_VERSION(2,18,0)
  // This is here just for pedagogical purposes, GDK_WINDOW_XID will call
  // it as well in newer Gtk versions
  if (!gdk_window_ensure_native (widget->window))
    g_error ("Couldn't create native window needed for GstXOverlay!");
#endif

#ifdef GDK_WINDOWING_X11
  video_window_xid = GDK_WINDOW_XID (gtk_widget_get_window (data->video_window));
#endif
}
   
/* This function is called when the PLAY button is clicked */
static void play_cb (GtkButton *button, CustomData *data) {
  gst_element_set_state (data->playbin2, GST_STATE_PLAYING);
}
   
/* This function is called when the PAUSE button is clicked */
static void pause_cb (GtkButton *button, CustomData *data) {
  gst_element_set_state (data->playbin2, GST_STATE_PAUSED);
}
   
/* This function is called when the STOP button is clicked */
static void stop_cb (GtkButton *button, CustomData *data) {
  gst_element_set_state (data->playbin2, GST_STATE_READY);
}
   
/* This function is called when the FILE OPEN button is clicked */
static void fileopen_cb (GtkButton *button, CustomData *data) {
  gst_element_set_state (data->playbin2, GST_STATE_READY);
  	GtkWidget *dialog;
	dialog = gtk_file_chooser_dialog_new ("Open File",
				      data->dialog_window,
				      GTK_FILE_CHOOSER_ACTION_OPEN,
				      GTK_STOCK_CANCEL, GTK_RESPONSE_CANCEL,
				      GTK_STOCK_OPEN, GTK_RESPONSE_ACCEPT,
				      NULL);
	if (gtk_dialog_run (GTK_DIALOG (dialog)) == GTK_RESPONSE_ACCEPT)
  	{
    char *filename;
    filename = gtk_file_chooser_get_filename (GTK_FILE_CHOOSER (dialog));
    g_print("File path %s \n",filename);
   	//File operations here
   	char *final_path;
   	int final_path_length =strlen("file://")+strlen(filename)+1;
   	final_path = malloc(final_path_length);
   	memset(final_path,0,final_path_length);
   	strcpy(final_path,"file://");
   	strcat(final_path,filename);
   	g_print(final_path);
   	//change the playbin uri to play the selected file
    g_object_set (data->playbin2, "uri", final_path, NULL);
    g_free (filename);
  	}
gtk_widget_destroy (dialog);
gst_element_set_state (data->playbin2, GST_STATE_PLAYING);
}


static void fastforward_cb (GtkButton *button, CustomData *data) {
    //g_print("FastFowardActivated!\n");

    if(data->rate != 2.0)
        data->rate = 2.0;
    else
        data->rate = 1.0;

    send_seek_event(data);
}


static void rewind_cb (GtkButton *button, CustomData *data) {
    
    //g_print("RewindActivated!\n");
    
    if(data->rate != -2.0)
        data->rate = -2.0;
    else
        data->rate = 1.0;

    send_seek_event(data);
}
/* This function is called when the main window is closed */
static void delete_event_cb (GtkWidget *widget, GdkEvent *event, CustomData *data) {
  stop_cb (NULL, data);
  gtk_main_quit ();
}
   
/* This function is called everytime the video window needs to be redrawn (due to damage/exposure,
 * rescaling, etc). GStreamer takes care of this in the PAUSED and PLAYING states, otherwise,
 * we simply draw a black rectangle to avoid garbage showing up. */
static gboolean expose_cb (GtkWidget *widget, GdkEventExpose *event, CustomData *data) {
  if (data->state < GST_STATE_PAUSED) {
    GtkAllocation allocation;
    GdkWindow *window = gtk_widget_get_window (widget);
    cairo_t *cr;
     
    /* Cairo is a 2D graphics library which we use here to clean the video window.
     * It is used by GStreamer for other reasons, so it will always be available to us. */
    gtk_widget_get_allocation (widget, &allocation);
    cr = gdk_cairo_create (window);
    cairo_set_source_rgb (cr, 0, 0, 0);
    cairo_rectangle (cr, 0, 0, allocation.width, allocation.height);
    cairo_fill (cr);
    cairo_destroy (cr);
  }
   
  return FALSE;
}
   
/* This function is called when the slider changes its position. We perform a seek to the
 * new position here. */
static void slider_cb (GtkRange *range, CustomData *data) {
  gdouble value = gtk_range_get_value (GTK_RANGE (data->slider));
  gst_element_seek_simple (data->playbin2, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_KEY_UNIT,
      (gint64)(value * GST_SECOND));
}
   
/* This creates all the GTK+ widgets that compose our application, and registers the callbacks */
static void create_ui (CustomData *data) {
  GtkWidget *main_window;  /* The uppermost window, containing all other windows */
  GtkWidget *video_window; /* The drawing area where the video will be shown */
  GtkWidget *main_box;     /* VBox to hold main_hbox and the controls */
  GtkWidget *main_hbox;    /* HBox to hold the video_window and the stream info text widget */
  GtkWidget *controls;     /* HBox to hold the buttons and the slider */
  GtkWidget *play_button, *pause_button, *stop_button, *fileopen_button, *fastforward_button, *fastrewind_button; /* Buttons */
  GtkWindow *dialog_window;
  main_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  dialog_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  data->dialog_window = dialog_window;
  
  //setting player properties
  gtk_window_set_title(GTK_WINDOW(main_window), "CS 414 MP 1 Player");
  g_signal_connect (G_OBJECT (main_window), "delete-event", G_CALLBACK (delete_event_cb), data);
  gtk_window_set_default_size(GTK_WINDOW(main_window), 230, 150);
  gtk_window_set_position(GTK_WINDOW(main_window), GTK_WIN_POS_CENTER);
  
  video_window = gtk_drawing_area_new ();
  data->video_window = video_window;
  gtk_widget_set_double_buffered (video_window, FALSE);
  g_signal_connect (video_window, "realize", G_CALLBACK (realize_cb), data);
  g_signal_connect (video_window, "expose_event", G_CALLBACK (expose_cb), data);
   
  play_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_PLAY);
  g_signal_connect (G_OBJECT (play_button), "clicked", G_CALLBACK (play_cb), data);
   
  pause_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_PAUSE);
  g_signal_connect (G_OBJECT (pause_button), "clicked", G_CALLBACK (pause_cb), data);
   
  stop_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_STOP);
  g_signal_connect (G_OBJECT (stop_button), "clicked", G_CALLBACK (stop_cb), data);
 
  fastforward_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_FORWARD);
  g_signal_connect (G_OBJECT (fastforward_button), "clicked", G_CALLBACK (fastforward_cb), data);

  fastrewind_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_REWIND);
  g_signal_connect (G_OBJECT (fastrewind_button), "clicked", G_CALLBACK (rewind_cb), data);

  //Button to open file
  fileopen_button = gtk_button_new_with_label ("Open File");
  g_signal_connect (G_OBJECT (fileopen_button), "clicked", G_CALLBACK (fileopen_cb), data);
  
  data->slider = gtk_hscale_new_with_range (0, 100, 1);
  gtk_scale_set_draw_value (GTK_SCALE (data->slider), 0);
  data->slider_update_signal_id = g_signal_connect (G_OBJECT (data->slider), "value-changed", G_CALLBACK (slider_cb), data);
   
  data->streams_list = gtk_text_view_new ();
  gtk_text_view_set_editable (GTK_TEXT_VIEW (data->streams_list), FALSE);
   
  controls = gtk_hbox_new (FALSE, 0);
  gtk_box_pack_start (GTK_BOX (controls), fastrewind_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), play_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), pause_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), stop_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), fastforward_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), play_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), fileopen_button, FALSE, FALSE, 2);
  gtk_box_pack_start (GTK_BOX (controls), data->slider, TRUE, TRUE, 2);
   
  main_hbox = gtk_hbox_new (FALSE, 0);
  gtk_box_pack_start (GTK_BOX (main_hbox), video_window, TRUE, TRUE, 0);
  gtk_box_pack_start (GTK_BOX (main_hbox), data->streams_list, FALSE, FALSE, 2);
   
  main_box = gtk_vbox_new (FALSE, 0);
  gtk_box_pack_start (GTK_BOX (main_box), main_hbox, TRUE, TRUE, 0);
  gtk_box_pack_start (GTK_BOX (main_box), controls, FALSE, FALSE, 0);
  gtk_container_add (GTK_CONTAINER (main_window), main_box);
  gtk_window_set_default_size (GTK_WINDOW (main_window), 640, 480);
  
  // usually the video_window will not be directly embedded into the
  // application window like this, but there will be many other widgets
  // and the video window will be embedded in one of them instead
  gtk_container_add (GTK_CONTAINER (main_window), video_window);
  
  gtk_widget_show_all (main_window);
  // realize window now so that the video window gets created and we can
  // obtain its XID before the pipeline is started up and the videosink
  // asks for the XID of the window to render onto
  gtk_widget_realize (video_window);

  // we should have the XID now
  g_assert (video_window_xid != 0);
  
}
   
/* This function is called periodically to refresh the GUI */
static gboolean refresh_ui (CustomData *data) {
  GstFormat fmt = GST_FORMAT_TIME;
  gint64 current = -1;
   
  /* We do not want to update anything unless we are in the PAUSED or PLAYING states */
  if (data->state < GST_STATE_PAUSED)
    return TRUE;
   
  /* If we didn't know it yet, query the stream duration */
  if (!GST_CLOCK_TIME_IS_VALID (data->duration)) {
    if (!gst_element_query_duration (data->playbin2, &fmt, &data->duration)) {
      g_printerr ("Could not query current duration.\n");
    } else {
      /* Set the range of the slider to the clip duration, in SECONDS */
      gtk_range_set_range (GTK_RANGE (data->slider), 0, (gdouble)data->duration / GST_SECOND);
    }
  }
   
  if (gst_element_query_position (data->playbin2, &fmt, &current)) {
    /* Block the "value-changed" signal, so the slider_cb function is not called
     * (which would trigger a seek the user has not requested) */
    g_signal_handler_block (data->slider, data->slider_update_signal_id);
    /* Set the position of the slider to the current pipeline positoin, in SECONDS */
    gtk_range_set_value (GTK_RANGE (data->slider), (gdouble)current / GST_SECOND);
    /* Re-enable the signal */
    g_signal_handler_unblock (data->slider, data->slider_update_signal_id);
  }
  return TRUE;
}
   
/* This function is called when new metadata is discovered in the stream */
static void tags_cb (GstElement *playbin2, gint stream, CustomData *data) {
  /* We are possibly in a GStreamer working thread, so we notify the main
   * thread of this event through a message in the bus */
  gst_element_post_message (playbin2,
    gst_message_new_application (GST_OBJECT (playbin2),
      gst_structure_new ("tags-changed", NULL)));
}
   
/* This function is called when an error message is posted on the bus */
static void error_cb (GstBus *bus, GstMessage *msg, CustomData *data) {
  GError *err;
  gchar *debug_info;
   
  /* Print error details on the screen */
  gst_message_parse_error (msg, &err, &debug_info);
  g_printerr ("Error received from element %s: %s\n", GST_OBJECT_NAME (msg->src), err->message);
  g_printerr ("Debugging information: %s\n", debug_info ? debug_info : "none");
  g_clear_error (&err);
  g_free (debug_info);
   
  /* Set the pipeline to READY (which stops playback) */
  gst_element_set_state (data->playbin2, GST_STATE_READY);
}
   
/* This function is called when an End-Of-Stream message is posted on the bus.
 * We just set the pipeline to READY (which stops playback) */
static void eos_cb (GstBus *bus, GstMessage *msg, CustomData *data) {
  g_print ("End-Of-Stream reached.\n");
  gst_element_set_state (data->playbin2, GST_STATE_READY);
}
   
/* This function is called when the pipeline changes states. We use it to
 * keep track of the current state. */
static void state_changed_cb (GstBus *bus, GstMessage *msg, CustomData *data) {
  GstState old_state, new_state, pending_state;
  gst_message_parse_state_changed (msg, &old_state, &new_state, &pending_state);
  if (GST_MESSAGE_SRC (msg) == GST_OBJECT (data->playbin2)) {
    data->state = new_state;
    g_print ("State set to %s\n", gst_element_state_get_name (new_state));
    if (old_state == GST_STATE_READY && new_state == GST_STATE_PAUSED) {
      /* For extra responsiveness, we refresh the GUI as soon as we reach the PAUSED state */
      refresh_ui (data);
    }
  }
}


/* Used by fast forward and fast rewind functions to change advance rate.  */
static void send_seek_event (CustomData *data) {
  gint64 position;
  GstFormat format = GST_FORMAT_TIME;
  GstEvent *seek_event;
   
  /* Obtain the current position, needed for the seek event */
  if (!gst_element_query_position (data->playbin2, &format, &position)) {
    g_printerr ("Unable to retrieve current position.\n");
    return;
  }
   
  /* Create the seek event */
  if (data->rate > 0) {
    seek_event = gst_event_new_seek (data->rate, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE,
        GST_SEEK_TYPE_SET, position, GST_SEEK_TYPE_NONE, 0);
  } else {
    seek_event = gst_event_new_seek (data->rate, GST_FORMAT_TIME, GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE,
        GST_SEEK_TYPE_SET, 0, GST_SEEK_TYPE_SET, position);
  }
   
  if (data->video_sink == NULL) {
    /* If we have not done so, obtain the sink through which we will send the seek events */
    g_object_get (data->playbin2, "video-sink", &data->video_sink, NULL);
  }
   
  /* Send the event */
  gst_element_send_event (data->video_sink, seek_event);
   
  g_print ("Current rate: %g\n", data->rate);
}

