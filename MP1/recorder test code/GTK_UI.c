#include "callbacks.c"

//ui function
static void create_ui()
{
	GtkWidget *video_window;
	GtkWidget *main_window;
	GtkWidget *main_box;
	GtkWidget *main_hbox;
	GtkWidget *controls;
	GtkWidget *slider;
	GtkWidget *radio_hbox;
	GtkWidget *audio_vbox;
	GtkWidget *video_vbox;
	GtkWidget *record_video_button, *record_audio_button;
	//player
	GtkWidget *play_button, *pause_button, *stop_button, *fileopen_button, *fastforward_button, *fastrewind_button;;
	GtkWidget *audio_encoding_1, *audio_encoding_2, *audio_encoding_3, *video_encoding_1, *video_encoding_2;
	GSList * audio_radio_buttons, *video_radio_buttons;
    GtkTextBuffer *buffer;
    GtkTextTagTable *table;
    
    //player
    dialog_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
    data.dialog_window = dialog_window;

	//make the main window and attach a delete window handler to it
	main_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
	gtk_window_set_title(GTK_WINDOW(main_window), "vPlayer");
	g_signal_connect(main_window, "delete-event", G_CALLBACK(delete_event_cb), NULL);
	
	//get window to hold actual video, attach the callback to store window xid when we can so we can connect the sink to it
	video_window = gtk_drawing_area_new ();
	g_signal_connect (video_window, "realize", G_CALLBACK (realize_cb), NULL);
	gtk_widget_set_double_buffered (video_window, FALSE);

	//initialize the buttons, attach callbacks
	record_video_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_RECORD);
	g_signal_connect(record_video_button, "clicked", G_CALLBACK(record_video_cb), NULL);
	gtk_button_set_label(record_video_button, "Record Video");
	record_audio_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_RECORD);
	g_signal_connect(record_audio_button, "clicked", G_CALLBACK(record_audio_cb), NULL);
	gtk_button_set_label(record_audio_button, "Record Audio");
	play_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_PLAY);
	g_signal_connect(play_button, "clicked", G_CALLBACK(play_cb), NULL);
	pause_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_PAUSE);
	g_signal_connect(pause_button, "clicked", G_CALLBACK(pause_cb), NULL);
	stop_button = gtk_button_new_from_stock(GTK_STOCK_MEDIA_STOP);
	g_signal_connect(stop_button, "clicked", G_CALLBACK(stop_cb), NULL);
	
	//player
	fastforward_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_FORWARD);
    g_signal_connect (G_OBJECT (fastforward_button), "clicked", G_CALLBACK (fastforward_cb), NULL);
    fastrewind_button = gtk_button_new_from_stock (GTK_STOCK_MEDIA_REWIND);
    g_signal_connect (G_OBJECT (fastrewind_button), "clicked", G_CALLBACK (rewind_cb), NULL);
    fileopen_button = gtk_button_new_with_label ("Open File");
    g_signal_connect (G_OBJECT (fileopen_button), "clicked", G_CALLBACK (fileopen_cb), NULL);  
    data.slider = gtk_hscale_new_with_range (0, 100, 1);
    gtk_scale_set_draw_value (GTK_SCALE (data.slider), 0);
    data.slider_update_signal_id = g_signal_connect (G_OBJECT (data.slider), "value-changed",  	G_CALLBACK (slider_cb), NULL);

    //Code for Monitor
    table = gtk_text_tag_table_new();
    buffer = gtk_text_buffer_new(table);
    gtk_text_buffer_set_text (buffer, "Attributes Monitor", -1);
    data.streams_list = gtk_text_view_new_with_buffer (buffer);
    gtk_text_view_set_editable (GTK_TEXT_VIEW (data.streams_list), FALSE);

	//radio buttons
	audio_encoding_1 = gtk_radio_button_new_with_label (NULL, "mu-law");
	audio_mulaw = audio_encoding_1;
	audio_radio_buttons = gtk_radio_button_get_group (GTK_RADIO_BUTTON (audio_encoding_1));
	audio_encoding_2 = gtk_radio_button_new_with_label (audio_radio_buttons, "a-law");
	audio_alaw = audio_encoding_2;
	audio_radio_buttons = gtk_radio_button_get_group (GTK_RADIO_BUTTON (audio_encoding_1));
	audio_encoding_3 = gtk_radio_button_new_with_label (audio_radio_buttons, "mkv");
	audio_mkv = audio_encoding_3;
	g_signal_connect(audio_encoding_1, "toggled", G_CALLBACK(audio_encoding_cb), NULL);
	g_signal_connect(audio_encoding_2, "toggled", G_CALLBACK(audio_encoding_cb), NULL);
	g_signal_connect(audio_encoding_3, "toggled", G_CALLBACK(audio_encoding_cb), NULL);

	video_encoding_1 = gtk_radio_button_new_with_label (NULL, "mjpeg");
	video_mjpeg = video_encoding_1;
	video_radio_buttons = gtk_radio_button_get_group (GTK_RADIO_BUTTON (video_encoding_1));
	video_encoding_2 = gtk_radio_button_new_with_label (video_radio_buttons, "mpeg");
	video_mpeg = video_encoding_2;
	g_signal_connect(video_encoding_1, "toggled", G_CALLBACK(video_encoding_cb), NULL);
	g_signal_connect(video_encoding_2, "toggled", G_CALLBACK(video_encoding_cb), NULL);

	//vbox for audio
	audio_vbox = gtk_vbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(audio_vbox), audio_encoding_1, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(audio_vbox), audio_encoding_2, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(audio_vbox), audio_encoding_3, FALSE, FALSE, 2);
	
	//vbox for audio
	video_vbox = gtk_vbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(video_vbox), video_encoding_1, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(video_vbox), video_encoding_2, FALSE, FALSE, 2);
	
	//hbox for the radio buttons
	radio_hbox = gtk_hbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(radio_hbox), audio_vbox, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(radio_hbox), video_vbox, FALSE, FALSE, 2);
	
	//initialize controls hbox and add buttons to it
	controls = gtk_hbox_new(FALSE, 0);
	gtk_box_pack_start (GTK_BOX (controls), fastrewind_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), play_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), pause_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), stop_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), fastforward_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), play_button, FALSE, FALSE, 2);
    gtk_box_pack_start (GTK_BOX (controls), fileopen_button, FALSE, FALSE, 2);
    gtk_box_pack_start(GTK_BOX(controls), record_video_button, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(controls), record_audio_button, FALSE, FALSE, 2);
    //gtk_box_pack_start (GTK_BOX (controls), data.slider, TRUE, TRUE, 2);

    slider = gtk_hbox_new(FALSE, 0);
    gtk_box_pack_start (GTK_BOX (slider), data.slider, TRUE, TRUE, 2);

	//initialize main hbox to hold video
	main_hbox = gtk_hbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(main_hbox), video_window, TRUE, TRUE, 0);
	gtk_box_pack_start (GTK_BOX (main_hbox), data.streams_list, FALSE, FALSE, 10);

	//initialize main vbox and put the control buttons and video in it
	main_box = gtk_vbox_new(FALSE, 0);
	gtk_box_pack_start(GTK_BOX(main_box), radio_hbox, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(main_box), main_hbox, TRUE, TRUE, 2);
	gtk_box_pack_start(GTK_BOX(main_box), controls, FALSE, FALSE, 2);
	gtk_box_pack_start(GTK_BOX(main_box), slider, FALSE, FALSE, 2);

	// usually the video_window will not be directly embedded into the
	// application window like this, but there will be many other widgets
	// and the video window will be embedded in one of them instead
	gtk_container_add (GTK_CONTAINER (main_window), main_box);
	gtk_window_set_default_size(GTK_WINDOW(main_window), 640, 480);
    gtk_container_add (GTK_CONTAINER (main_window), video_window);
    
	// show the GUI
	gtk_widget_show_all (main_window);

	// realize window now so that the video window gets created and we can
	// obtain its XID before the pipeline is started up and the videosink
	// asks for the XID of the window to render onto
	gtk_widget_realize (video_window);

	// we should have the XID now
	g_assert (video_window_xid != 0);
}
