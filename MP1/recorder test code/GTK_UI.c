#include "callbacks.c"

//ui function
static void create_ui()
{
	GtkWidget *video_window;
	GtkWidget *main_window;
	GtkWidget *main_box;
	GtkWidget *main_hbox;
	GtkWidget *controls;
	GtkWidget *record_video_button, *record_audio_button, *play_button, *pause_button, *stop_button;
  
	//make the main window and attach a delete window handler to it
	main_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
	g_signal_connect(main_window, "delete-event", G_CALLBACK(delete_event_cb), NULL);
	
	//get window to hold actual video, attach the callback to store window xid when we can so we can connect the sink to it
	video_window = gtk_drawing_area_new ();
	g_signal_connect (video_window, "realize", G_CALLBACK (realize_cb), NULL);
	gtk_widget_set_double_buffered (video_window, FALSE);

	//initialize the buttons, attach callbacks
	record_video_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_RECORD);
	g_signal_connect(record_video_button, "clicked", G_CALLBACK(record_video_cb), NULL);
	record_audio_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_RECORD);
	g_signal_connect(record_audio_button, "clicked", G_CALLBACK(record_audio_cb), NULL);
	play_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_PLAY);
	g_signal_connect(play_button, "clicked", G_CALLBACK(play_cb), NULL);
	pause_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_PAUSE);
	g_signal_connect(pause_button, "clicked", G_CALLBACK(pause_cb), NULL);
	stop_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_STOP);
	g_signal_connect(stop_button, "clicked", G_CALLBACK(stop_cb), NULL);

	//initialize controls hbox and add buttons to it
	controls = gtk_hbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(controls), record_video_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), record_audio_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), record_audio_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), play_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), pause_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), stop_button, FALSE, FALSE, 2);

	//initialize main hbox to hold video
	main_hbox = gtk_hbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(main_hbox), video_window, TRUE, TRUE, 0);

	//initialize main vbox and put the control buttons and video in it
	main_box = gtk_vbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(main_box), main_hbox, TRUE, TRUE, 2);
	gtk_box_pack_start(GTK_BOX(main_box), controls, FALSE, FALSE, 2);

	// usually the video_window will not be directly embedded into the
	// application window like this, but there will be many other widgets
	// and the video window will be embedded in one of them instead
	gtk_container_add (GTK_CONTAINER (main_window), main_box);
	gtk_window_set_default_size(GTK_WINDOW(main_window), 640, 480);

	// show the GUI
	gtk_widget_show_all (main_window);

	// realize window now so that the video window gets created and we can
	// obtain its XID before the pipeline is started up and the videosink
	// asks for the XID of the window to render onto
	gtk_widget_realize (video_window);

	// we should have the XID now
	g_assert (video_window_xid != 0);
}
