#include <gst/gst.h>
#include <gtk/gtk.h>
#include <gst/interfaces/xoverlay.h>
#include <string.h>
#include <gdk/gdk.h>
#include <gdk/gdkwin32.h>
#include <glib.h>

static gintptr video_window_xid = 0;
static gboolean change_request = FALSE;
static GtkWidget* audio_alaw, *audio_mulaw, *video_mjpeg, *video_mpeg;

typedef enum {STREAM, RECORD_VIDEO, RECORD_AUDIO} mode;
typedef enum {ALAW, MULAW} AudioEncoder;
typedef enum {MJPEG, MPEG} VideoEncoder;

typedef struct _CustomData
{
	GstBus *bus;
	GstElement *pipeline;
	GstElement *source;
	GstCaps *enc_caps;
	GstElement *colorspace;
	GstElement *encoder;
	GstElement *mux;
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
} CustomData;
