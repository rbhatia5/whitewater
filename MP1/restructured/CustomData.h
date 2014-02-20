/*#include <gst/gst.h>
#include <gtk/gtk.h>
#include <gst/interfaces/xoverlay.h>
#include <string.h>
#include <gdk/gdk.h>
#include <gdk/gdkwin32.h>
#include <glib.h>
*/

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
#include <stdio.h>
#include <stdlib.h>
#endif

static gintptr video_window_xid = 0;
static gboolean change_request = FALSE;
static GtkWidget* audio_alaw, *audio_mulaw, *audio_mkv, *video_mjpeg, *video_mpeg;

typedef enum {RECORDER, PLAYER} mode;
typedef enum {ALAW, MULAW, MKV} AudioEncoder;
typedef enum {MJPEG, MPEG} VideoEncoder;

typedef struct _CustomData {
	GstBus *bus;
	GstElement *pipeline;
	GstElement *source;
	GstCaps *enc_caps;
	GstElement *colorspace;
	GstElement *encoder;
	GstElement *decoder;
	GstElement *mux;
	GstElement *audioconvert;
	GstElement *audiosink;
	GstElement *sink;
	GstElement *sink2;
	GstBin * custom_bin;
	GstElement *tee;
	GstElement *player_queue;
	GstElement *file_queue;
	GstState state;
	mode Mode;
	AudioEncoder audio_encoder;
	VideoEncoder video_encoder;
	GtkWidget *slider;              /* Slider widget to keep track of current position */
    GtkWidget *streams_list;        /* Text widget to display info about the streams */
    GtkWindow *main_window;
    gulong slider_update_signal_id; /* Signal ID for the slider update signal */	
    gint64 duration;                /* Duration of the clip, in nanoseconds */
    gdouble rate;
    GstElement *video_sink;
} CustomData;

typedef struct _Monitor{

	GstElement *tee1, *q1a, *q1b;
	GstPad *tpad1a, *tpad1b;

	GstElement *tee2, *q2a, *q2b;
	GstPad *tpad2a, *tpad2b;

	GstPad *q1aPado,*q1bPado,*q2aPado,*q2bPado;
	GstPad *q1aPadi,*q1bPadi,*q2aPadi,*q2bPadi;
	GstElement *appsinki, *appsinkf;

}Monitor;


typedef struct _PlayerControls {
GtkWidget *audio_vbox;
GtkWidget *video_vbox;
GtkWidget *recorder_button, *player_button;
GtkWidget *record_video_button, *record_audio_button;
GtkWidget *play_button, *pause_button, *stop_button, *fileopen_button, *fastforward_button, *fastrewind_button;	
} PlayerControls;

  
  GtkWidget *video_window;		  /* Main window of the UI */
  GtkWindow *dialog_window;

