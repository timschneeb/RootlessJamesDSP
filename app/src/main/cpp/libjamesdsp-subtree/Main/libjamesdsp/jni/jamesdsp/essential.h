#ifndef _ANDROID_EFFECT_HEADER_
#define _ANDROID_EFFECT_HEADER_
#include <errno.h>
typedef struct effect_uuid_s
{
    uint32_t timeLow;
    uint16_t timeMid;
    uint16_t timeHiAndVersion;
    uint16_t clockSeq;
    uint8_t node[6];
} effect_uuid_t;

// Maximum length of character strings in structures defines by this API.
#define EFFECT_STRING_LEN_MAX 64

// NULL UUID definition (matches SL_IID_NULL_)
#define EFFECT_UUID_INITIALIZER { 0xec7178ec, 0xe5e1, 0x4432, 0xa3f4, \
                                  { 0x46, 0x57, 0xe6, 0x79, 0x52, 0x10 } }
static const effect_uuid_t EFFECT_UUID_NULL_ = EFFECT_UUID_INITIALIZER;
static const effect_uuid_t * const EFFECT_UUID_NULL = &EFFECT_UUID_NULL_;
static const char * const EFFECT_UUID_NULL_STR = "ec7178ec-e5e1-4432-a3f4-4657e6795210";


// The effect descriptor contains necessary information to facilitate the enumeration of the effect
// engines present in a library.
typedef struct effect_descriptor_s
{
    effect_uuid_t type;     // UUID of to the OpenSL ES interface implemented by this effect
    effect_uuid_t uuid;     // UUID for this particular implementation
    uint32_t apiVersion;    // Version of the effect control API implemented
    uint32_t flags;         // effect engine capabilities/requirements flags (see below)
    uint16_t cpuLoad;       // CPU load indication (see below)
    uint16_t memoryUsage;   // Data Memory usage (see below)
    char    name[EFFECT_STRING_LEN_MAX];   // human readable effect name
    char    implementor[EFFECT_STRING_LEN_MAX];    // human readable effect implementor name
} effect_descriptor_t;

// CPU load and memory usage indication: each effect implementation must provide an indication of
// its CPU and memory usage for the audio effect framework to limit the number of effects
// instantiated at a given time on a given platform.
// The CPU load is expressed in 0.1 MIPS units as estimated on an ARM9E core (ARMv5TE) with 0 WS.
// The memory usage is expressed in KB and includes only dynamically allocated memory

// Definitions for flags field of effect descriptor.
//  +---------------------------+-----------+-----------------------------------
//  | description               | bits      | values
//  +---------------------------+-----------+-----------------------------------
//  | connection mode           | 0..2      | 0 insert: after track process
//  |                           |           | 1 auxiliary: connect to track auxiliary
//  |                           |           |  output and use send level
//  |                           |           | 2 replace: replaces track process function;
//  |                           |           |   must implement SRC, volume and mono to stereo.
//  |                           |           | 3 pre processing: applied below audio HAL on input
//  |                           |           | 4 post processing: applied below audio HAL on output
//  |                           |           | 5 - 7 reserved
//  +---------------------------+-----------+-----------------------------------
//  | insertion preference      | 3..5      | 0 none
//  |                           |           | 1 first of the chain
//  |                           |           | 2 last of the chain
//  |                           |           | 3 exclusive (only effect in the insert chain)
//  |                           |           | 4..7 reserved
//  +---------------------------+-----------+-----------------------------------
//  | Volume management         | 6..8      | 0 none
//  |                           |           | 1 implements volume control
//  |                           |           | 2 requires volume indication
//  |                           |           | 4 reserved
//  +---------------------------+-----------+-----------------------------------
//  | Device indication         | 9..11     | 0 none
//  |                           |           | 1 requires device updates
//  |                           |           | 2, 4 reserved
//  +---------------------------+-----------+-----------------------------------
//  | Sample input mode         | 12..13    | 1 direct: process() function or EFFECT_CMD_SET_CONFIG
//  |                           |           |   command must specify a buffer descriptor
//  |                           |           | 2 provider: process() function uses the
//  |                           |           |   bufferProvider indicated by the
//  |                           |           |   EFFECT_CMD_SET_CONFIG command to request input.
//  |                           |           |   buffers.
//  |                           |           | 3 both: both input modes are supported
//  +---------------------------+-----------+-----------------------------------
//  | Sample output mode        | 14..15    | 1 direct: process() function or EFFECT_CMD_SET_CONFIG
//  |                           |           |   command must specify a buffer descriptor
//  |                           |           | 2 provider: process() function uses the
//  |                           |           |   bufferProvider indicated by the
//  |                           |           |   EFFECT_CMD_SET_CONFIG command to request output
//  |                           |           |   buffers.
//  |                           |           | 3 both: both output modes are supported
//  +---------------------------+-----------+-----------------------------------
//  | Hardware acceleration     | 16..17    | 0 No hardware acceleration
//  |                           |           | 1 non tunneled hw acceleration: the process() function
//  |                           |           |   reads the samples, send them to HW accelerated
//  |                           |           |   effect processor, reads back the processed samples
//  |                           |           |   and returns them to the output buffer.
//  |                           |           | 2 tunneled hw acceleration: the process() function is
//  |                           |           |   transparent. The effect interface is only used to
//  |                           |           |   control the effect engine. This mode is relevant for
//  |                           |           |   global effects actually applied by the audio
//  |                           |           |   hardware on the output stream.
//  +---------------------------+-----------+-----------------------------------
//  | Audio Mode indication     | 18..19    | 0 none
//  |                           |           | 1 requires audio mode updates
//  |                           |           | 2..3 reserved
//  +---------------------------+-----------+-----------------------------------
//  | Audio source indication   | 20..21    | 0 none
//  |                           |           | 1 requires audio source updates
//  |                           |           | 2..3 reserved
//  +---------------------------+-----------+-----------------------------------
//  | Effect offload supported  | 22        | 0 The effect cannot be offloaded to an audio DSP
//  |                           |           | 1 The effect can be offloaded to an audio DSP
//  +---------------------------+-----------+-----------------------------------

// Insert mode
#define EFFECT_FLAG_TYPE_SHIFT          0
#define EFFECT_FLAG_TYPE_SIZE           3
#define EFFECT_FLAG_TYPE_MASK           (((1 << EFFECT_FLAG_TYPE_SIZE) -1) \
                                            << EFFECT_FLAG_TYPE_SHIFT)
#define EFFECT_FLAG_TYPE_INSERT         (0 << EFFECT_FLAG_TYPE_SHIFT)
#define EFFECT_FLAG_TYPE_AUXILIARY      (1 << EFFECT_FLAG_TYPE_SHIFT)
#define EFFECT_FLAG_TYPE_REPLACE        (2 << EFFECT_FLAG_TYPE_SHIFT)
#define EFFECT_FLAG_TYPE_PRE_PROC       (3 << EFFECT_FLAG_TYPE_SHIFT)
#define EFFECT_FLAG_TYPE_POST_PROC      (4 << EFFECT_FLAG_TYPE_SHIFT)

// Insert preference
#define EFFECT_FLAG_INSERT_SHIFT        (EFFECT_FLAG_TYPE_SHIFT + EFFECT_FLAG_TYPE_SIZE)
#define EFFECT_FLAG_INSERT_SIZE         3
#define EFFECT_FLAG_INSERT_MASK         (((1 << EFFECT_FLAG_INSERT_SIZE) -1) \
                                            << EFFECT_FLAG_INSERT_SHIFT)
#define EFFECT_FLAG_INSERT_ANY          (0 << EFFECT_FLAG_INSERT_SHIFT)
#define EFFECT_FLAG_INSERT_FIRST        (1 << EFFECT_FLAG_INSERT_SHIFT)
#define EFFECT_FLAG_INSERT_LAST         (2 << EFFECT_FLAG_INSERT_SHIFT)
#define EFFECT_FLAG_INSERT_EXCLUSIVE    (3 << EFFECT_FLAG_INSERT_SHIFT)


// Volume control
#define EFFECT_FLAG_VOLUME_SHIFT        (EFFECT_FLAG_INSERT_SHIFT + EFFECT_FLAG_INSERT_SIZE)
#define EFFECT_FLAG_VOLUME_SIZE         3
#define EFFECT_FLAG_VOLUME_MASK         (((1 << EFFECT_FLAG_VOLUME_SIZE) -1) \
                                            << EFFECT_FLAG_VOLUME_SHIFT)
#define EFFECT_FLAG_VOLUME_CTRL         (1 << EFFECT_FLAG_VOLUME_SHIFT)
#define EFFECT_FLAG_VOLUME_IND          (2 << EFFECT_FLAG_VOLUME_SHIFT)
#define EFFECT_FLAG_VOLUME_NONE         (0 << EFFECT_FLAG_VOLUME_SHIFT)

// Device indication
#define EFFECT_FLAG_DEVICE_SHIFT        (EFFECT_FLAG_VOLUME_SHIFT + EFFECT_FLAG_VOLUME_SIZE)
#define EFFECT_FLAG_DEVICE_SIZE         3
#define EFFECT_FLAG_DEVICE_MASK         (((1 << EFFECT_FLAG_DEVICE_SIZE) -1) \
                                            << EFFECT_FLAG_DEVICE_SHIFT)
#define EFFECT_FLAG_DEVICE_IND          (1 << EFFECT_FLAG_DEVICE_SHIFT)
#define EFFECT_FLAG_DEVICE_NONE         (0 << EFFECT_FLAG_DEVICE_SHIFT)

// Sample input modes
#define EFFECT_FLAG_INPUT_SHIFT         (EFFECT_FLAG_DEVICE_SHIFT + EFFECT_FLAG_DEVICE_SIZE)
#define EFFECT_FLAG_INPUT_SIZE          2
#define EFFECT_FLAG_INPUT_MASK          (((1 << EFFECT_FLAG_INPUT_SIZE) -1) \
                                            << EFFECT_FLAG_INPUT_SHIFT)
#define EFFECT_FLAG_INPUT_DIRECT        (1 << EFFECT_FLAG_INPUT_SHIFT)
#define EFFECT_FLAG_INPUT_PROVIDER      (2 << EFFECT_FLAG_INPUT_SHIFT)
#define EFFECT_FLAG_INPUT_BOTH          (3 << EFFECT_FLAG_INPUT_SHIFT)

// Sample output modes
#define EFFECT_FLAG_OUTPUT_SHIFT        (EFFECT_FLAG_INPUT_SHIFT + EFFECT_FLAG_INPUT_SIZE)
#define EFFECT_FLAG_OUTPUT_SIZE         2
#define EFFECT_FLAG_OUTPUT_MASK         (((1 << EFFECT_FLAG_OUTPUT_SIZE) -1) \
                                            << EFFECT_FLAG_OUTPUT_SHIFT)
#define EFFECT_FLAG_OUTPUT_DIRECT       (1 << EFFECT_FLAG_OUTPUT_SHIFT)
#define EFFECT_FLAG_OUTPUT_PROVIDER     (2 << EFFECT_FLAG_OUTPUT_SHIFT)
#define EFFECT_FLAG_OUTPUT_BOTH         (3 << EFFECT_FLAG_OUTPUT_SHIFT)

// Hardware acceleration mode
#define EFFECT_FLAG_HW_ACC_SHIFT        (EFFECT_FLAG_OUTPUT_SHIFT + EFFECT_FLAG_OUTPUT_SIZE)
#define EFFECT_FLAG_HW_ACC_SIZE         2
#define EFFECT_FLAG_HW_ACC_MASK         (((1 << EFFECT_FLAG_HW_ACC_SIZE) -1) \
                                            << EFFECT_FLAG_HW_ACC_SHIFT)
#define EFFECT_FLAG_HW_ACC_SIMPLE       (1 << EFFECT_FLAG_HW_ACC_SHIFT)
#define EFFECT_FLAG_HW_ACC_TUNNEL       (2 << EFFECT_FLAG_HW_ACC_SHIFT)

// Audio mode indication
#define EFFECT_FLAG_AUDIO_MODE_SHIFT    (EFFECT_FLAG_HW_ACC_SHIFT + EFFECT_FLAG_HW_ACC_SIZE)
#define EFFECT_FLAG_AUDIO_MODE_SIZE     2
#define EFFECT_FLAG_AUDIO_MODE_MASK     (((1 << EFFECT_FLAG_AUDIO_MODE_SIZE) -1) \
                                            << EFFECT_FLAG_AUDIO_MODE_SHIFT)
#define EFFECT_FLAG_AUDIO_MODE_IND      (1 << EFFECT_FLAG_AUDIO_MODE_SHIFT)
#define EFFECT_FLAG_AUDIO_MODE_NONE     (0 << EFFECT_FLAG_AUDIO_MODE_SHIFT)

// Audio source indication
#define EFFECT_FLAG_AUDIO_SOURCE_SHIFT  (EFFECT_FLAG_AUDIO_MODE_SHIFT + EFFECT_FLAG_AUDIO_MODE_SIZE)
#define EFFECT_FLAG_AUDIO_SOURCE_SIZE   2
#define EFFECT_FLAG_AUDIO_SOURCE_MASK   (((1 << EFFECT_FLAG_AUDIO_SOURCE_SIZE) -1) \
                                          << EFFECT_FLAG_AUDIO_SOURCE_SHIFT)
#define EFFECT_FLAG_AUDIO_SOURCE_IND    (1 << EFFECT_FLAG_AUDIO_SOURCE_SHIFT)
#define EFFECT_FLAG_AUDIO_SOURCE_NONE   (0 << EFFECT_FLAG_AUDIO_SOURCE_SHIFT)

// Effect offload indication
#define EFFECT_FLAG_OFFLOAD_SHIFT       (EFFECT_FLAG_AUDIO_SOURCE_SHIFT + \
                                                    EFFECT_FLAG_AUDIO_SOURCE_SIZE)
#define EFFECT_FLAG_OFFLOAD_SIZE        1
#define EFFECT_FLAG_OFFLOAD_MASK        (((1 << EFFECT_FLAG_OFFLOAD_SIZE) -1) \
                                          << EFFECT_FLAG_OFFLOAD_SHIFT)
#define EFFECT_FLAG_OFFLOAD_SUPPORTED   (1 << EFFECT_FLAG_OFFLOAD_SHIFT)

#define EFFECT_MAKE_API_VERSION(M, m)  (((M)<<16) | ((m) & 0xFFFF))
#define EFFECT_API_VERSION_MAJOR(v)    ((v)>>16)
#define EFFECT_API_VERSION_MINOR(v)    ((m) & 0xFFFF)



/////////////////////////////////////////////////
//      Effect control interface
/////////////////////////////////////////////////

// Effect control interface version 2.0
#define EFFECT_CONTROL_API_VERSION EFFECT_MAKE_API_VERSION(2,0)

// Effect control interface structure: effect_interface_s
// The effect control interface is exposed by each effect engine implementation. It consists of
// a set of functions controlling the configuration, activation and process of the engine.
// The functions are grouped in a structure of type effect_interface_s.
//
// Effect control interface handle: effect_handle_t
// The effect_handle_t serves two purposes regarding the implementation of the effect engine:
// - 1 it is the address of a pointer to an effect_interface_s structure where the functions
// of the effect control API for a particular effect are located.
// - 2 it is the address of the context of a particular effect instance.
// A typical implementation in the effect library would define a structure as follows:
// struct effect_module_s {
//        const struct effect_interface_s *itfe;
//        effect_config_t config;
//        effect_context_t context;
// }
// The implementation of EffectCreate() function would then allocate a structure of this
// type and return its address as effect_handle_t
typedef struct effect_interface_s **effect_handle_t;


// Forward definition of type audio_buffer_t
typedef struct audio_buffer_s audio_buffer_t;

// Effect control interface definition
struct effect_interface_s
{
    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:       process
    //
    //    Description:    Effect process function. Takes input samples as specified
    //          (count and location) in input buffer descriptor and output processed
    //          samples as specified in output buffer descriptor. If the buffer descriptor
    //          is not specified the function must use either the buffer or the
    //          buffer provider function installed by the EFFECT_CMD_SET_CONFIG command.
    //          The effect framework will call the process() function after the EFFECT_CMD_ENABLE
    //          command is received and until the EFFECT_CMD_DISABLE is received. When the engine
    //          receives the EFFECT_CMD_DISABLE command it should turn off the effect gracefully
    //          and when done indicate that it is OK to stop calling the process() function by
    //          returning the -ENODATA status.
    //
    //    NOTE: the process() function implementation should be "real-time safe" that is
    //      it should not perform blocking calls: malloc/free, sleep, read/write/open/close,
    //      pthread_cond_wait/pthread_mutex_lock...
    //
    //    Input:
    //          self:       handle to the effect interface this function
    //              is called on.
    //          inBuffer:   buffer descriptor indicating where to read samples to process.
    //              If NULL, use the configuration passed by EFFECT_CMD_SET_CONFIG command.
    //
    //          outBuffer:   buffer descriptor indicating where to write processed samples.
    //              If NULL, use the configuration passed by EFFECT_CMD_SET_CONFIG command.
    //
    //    Output:
    //        returned value:    0 successful operation
    //                          -ENODATA the engine has finished the disable phase and the framework
    //                                  can stop calling process()
    //                          -EINVAL invalid interface handle or
    //                                  invalid input/output buffer description
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*process)(effect_handle_t self,
                       audio_buffer_t *inBuffer,
                       audio_buffer_t *outBuffer);
    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:       command
    //
    //    Description:    Send a command and receive a response to/from effect engine.
    //
    //    Input:
    //          self:       handle to the effect interface this function
    //              is called on.
    //          cmdCode:    command code: the command can be a standardized command defined in
    //              effect_command_e (see below) or a proprietary command.
    //          cmdSize:    size of command in bytes
    //          pCmdData:   pointer to command data
    //          pReplyData: pointer to reply data
    //
    //    Input/Output:
    //          replySize: maximum size of reply data as input
    //                      actual size of reply data as output
    //
    //    Output:
    //          returned value: 0       successful operation
    //                          -EINVAL invalid interface handle or
    //                                  invalid command/reply size or format according to command code
    //              The return code should be restricted to indicate problems related to the this
    //              API specification. Status related to the execution of a particular command should be
    //              indicated as part of the reply field.
    //
    //          *pReplyData updated with command response
    //
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*command)(effect_handle_t self,
                       uint32_t cmdCode,
                       uint32_t cmdSize,
                       void *pCmdData,
                       uint32_t *replySize,
                       void *pReplyData);
    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:        get_descriptor
    //
    //    Description:    Returns the effect descriptor
    //
    //    Input:
    //          self:       handle to the effect interface this function
    //              is called on.
    //
    //    Input/Output:
    //          pDescriptor:    address where to return the effect descriptor.
    //
    //    Output:
    //        returned value:    0          successful operation.
    //                          -EINVAL     invalid interface handle or invalid pDescriptor
    //        *pDescriptor:     updated with the effect descriptor.
    //
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*get_descriptor)(effect_handle_t self,
                              effect_descriptor_t *pDescriptor);
    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:       process_reverse
    //
    //    Description:    Process reverse stream function. This function is used to pass
    //          a reference stream to the effect engine. If the engine does not need a reference
    //          stream, this function pointer can be set to NULL.
    //          This function would typically implemented by an Echo Canceler.
    //
    //    Input:
    //          self:       handle to the effect interface this function
    //              is called on.
    //          inBuffer:   buffer descriptor indicating where to read samples to process.
    //              If NULL, use the configuration passed by EFFECT_CMD_SET_CONFIG_REVERSE command.
    //
    //          outBuffer:   buffer descriptor indicating where to write processed samples.
    //              If NULL, use the configuration passed by EFFECT_CMD_SET_CONFIG_REVERSE command.
    //              If the buffer and buffer provider in the configuration received by
    //              EFFECT_CMD_SET_CONFIG_REVERSE are also NULL, do not return modified reverse
    //              stream data
    //
    //    Output:
    //        returned value:    0 successful operation
    //                          -ENODATA the engine has finished the disable phase and the framework
    //                                  can stop calling process_reverse()
    //                          -EINVAL invalid interface handle or
    //                                  invalid input/output buffer description
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*process_reverse)(effect_handle_t self,
                               audio_buffer_t *inBuffer,
                               audio_buffer_t *outBuffer);
};

//
//--- Standardized command codes for command() function
//
enum effect_command_e
{
    EFFECT_CMD_INIT,                 // initialize effect engine
    EFFECT_CMD_SET_CONFIG,           // configure effect engine (see effect_config_t)
    EFFECT_CMD_RESET,                // reset effect engine
    EFFECT_CMD_ENABLE,               // enable effect process
    EFFECT_CMD_DISABLE,              // disable effect process
    EFFECT_CMD_SET_PARAM,            // set parameter immediately (see effect_param_t)
    EFFECT_CMD_SET_PARAM_DEFERRED,   // set parameter deferred
    EFFECT_CMD_SET_PARAM_COMMIT,     // commit previous set parameter deferred
    EFFECT_CMD_GET_PARAM,            // get parameter
    EFFECT_CMD_SET_DEVICE,           // set audio device (see audio.h, audio_devices_t)
    EFFECT_CMD_SET_VOLUME,           // set volume
    EFFECT_CMD_SET_AUDIO_MODE,       // set the audio mode (normal, ring, ...)
    EFFECT_CMD_SET_CONFIG_REVERSE,   // configure effect engine reverse stream(see effect_config_t)
    EFFECT_CMD_SET_INPUT_DEVICE,     // set capture device (see audio.h, audio_devices_t)
    EFFECT_CMD_GET_CONFIG,           // read effect engine configuration
    EFFECT_CMD_GET_CONFIG_REVERSE,   // read configure effect engine reverse stream configuration
    EFFECT_CMD_GET_FEATURE_SUPPORTED_CONFIGS,// get all supported configurations for a feature.
    EFFECT_CMD_GET_FEATURE_CONFIG,   // get current feature configuration
    EFFECT_CMD_SET_FEATURE_CONFIG,   // set current feature configuration
    EFFECT_CMD_SET_AUDIO_SOURCE,     // set the audio source (see audio.h, audio_source_t)
    EFFECT_CMD_OFFLOAD,              // set if effect thread is an offload one,
    // send the ioHandle of the effect thread
    EFFECT_CMD_FIRST_PROPRIETARY = 0x10000 // first proprietary command code
};

//==================================================================================================
// command: EFFECT_CMD_INIT
//--------------------------------------------------------------------------------------------------
// description:
//  Initialize effect engine: All configurations return to default
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_SET_CONFIG
//--------------------------------------------------------------------------------------------------
// description:
//  Apply new audio parameters configurations for input and output buffers
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_config_t)
//  data: effect_config_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_RESET
//--------------------------------------------------------------------------------------------------
// description:
//  Reset the effect engine. Keep configuration but resets state and buffer content
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_ENABLE
//--------------------------------------------------------------------------------------------------
// description:
//  Enable the process. Called by the framework before the first call to process()
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_DISABLE
//--------------------------------------------------------------------------------------------------
// description:
//  Disable the process. Called by the framework after the last call to process()
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_SET_PARAM
//--------------------------------------------------------------------------------------------------
// description:
//  Set a parameter and apply it immediately
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_param_t) + size of param and value
//  data: effect_param_t + param + value. See effect_param_t definition below for value offset
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_SET_PARAM_DEFERRED
//--------------------------------------------------------------------------------------------------
// description:
//  Set a parameter but apply it only when receiving EFFECT_CMD_SET_PARAM_COMMIT command
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_param_t) + size of param and value
//  data: effect_param_t + param + value. See effect_param_t definition below for value offset
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_SET_PARAM_COMMIT
//--------------------------------------------------------------------------------------------------
// description:
//  Apply all previously received EFFECT_CMD_SET_PARAM_DEFERRED commands
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_GET_PARAM
//--------------------------------------------------------------------------------------------------
// description:
//  Get a parameter value
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_param_t) + size of param
//  data: effect_param_t + param
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(effect_param_t) + size of param and value
//  data: effect_param_t + param + value. See effect_param_t definition below for value offset
//==================================================================================================
// command: EFFECT_CMD_SET_DEVICE
//--------------------------------------------------------------------------------------------------
// description:
//  Set the rendering device the audio output path is connected to. See audio.h, audio_devices_t
//  for device values.
//  The effect implementation must set EFFECT_FLAG_DEVICE_IND flag in its descriptor to receive this
//  command when the device changes
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t)
//  data: uint32_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_SET_VOLUME
//--------------------------------------------------------------------------------------------------
// description:
//  Set and get volume. Used by audio framework to delegate volume control to effect engine.
//  The effect implementation must set EFFECT_FLAG_VOLUME_IND or EFFECT_FLAG_VOLUME_CTRL flag in
//  its descriptor to receive this command before every call to process() function
//  If EFFECT_FLAG_VOLUME_CTRL flag is set in the effect descriptor, the effect engine must return
//  the volume that should be applied before the effect is processed. The overall volume (the volume
//  actually applied by the effect engine multiplied by the returned value) should match the value
//  indicated in the command.
//--------------------------------------------------------------------------------------------------
// command format:
//  size: n * sizeof(uint32_t)
//  data: volume for each channel defined in effect_config_t for output buffer expressed in
//      8.24 fixed point format
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: n * sizeof(uint32_t) / 0
//  data: - if EFFECT_FLAG_VOLUME_CTRL is set in effect descriptor:
//              volume for each channel defined in effect_config_t for output buffer expressed in
//              8.24 fixed point format
//        - if EFFECT_FLAG_VOLUME_CTRL is not set in effect descriptor:
//              N/A
//  It is legal to receive a null pointer as pReplyData in which case the effect framework has
//  delegated volume control to another effect
//==================================================================================================
// command: EFFECT_CMD_SET_AUDIO_MODE
//--------------------------------------------------------------------------------------------------
// description:
//  Set the audio mode. The effect implementation must set EFFECT_FLAG_AUDIO_MODE_IND flag in its
//  descriptor to receive this command when the audio mode changes.
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t)
//  data: audio_mode_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_SET_CONFIG_REVERSE
//--------------------------------------------------------------------------------------------------
// description:
//  Apply new audio parameters configurations for input and output buffers of reverse stream.
//  An example of reverse stream is the echo reference supplied to an Acoustic Echo Canceler.
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_config_t)
//  data: effect_config_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(int)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_SET_INPUT_DEVICE
//--------------------------------------------------------------------------------------------------
// description:
//  Set the capture device the audio input path is connected to. See audio.h, audio_devices_t
//  for device values.
//  The effect implementation must set EFFECT_FLAG_DEVICE_IND flag in its descriptor to receive this
//  command when the device changes
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t)
//  data: uint32_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_GET_CONFIG
//--------------------------------------------------------------------------------------------------
// description:
//  Read audio parameters configurations for input and output buffers
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(effect_config_t)
//  data: effect_config_t
//==================================================================================================
// command: EFFECT_CMD_GET_CONFIG_REVERSE
//--------------------------------------------------------------------------------------------------
// description:
//  Read audio parameters configurations for input and output buffers of reverse stream
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 0
//  data: N/A
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(effect_config_t)
//  data: effect_config_t
//==================================================================================================
// command: EFFECT_CMD_GET_FEATURE_SUPPORTED_CONFIGS
//--------------------------------------------------------------------------------------------------
// description:
//  Queries for supported configurations for a particular feature (e.g. get the supported
// combinations of main and auxiliary channels for a noise suppressor).
// The command parameter is the feature identifier (See effect_feature_e for a list of defined
// features) followed by the maximum number of configuration descriptor to return.
// The reply is composed of:
//  - status (uint32_t):
//          - 0 if feature is supported
//          - -ENOSYS if the feature is not supported,
//          - -ENOMEM if the feature is supported but the total number of supported configurations
//          exceeds the maximum number indicated by the caller.
//  - total number of supported configurations (uint32_t)
//  - an array of configuration descriptors.
// The actual number of descriptors returned must not exceed the maximum number indicated by
// the caller.
//--------------------------------------------------------------------------------------------------
// command format:
//  size: 2 x sizeof(uint32_t)
//  data: effect_feature_e + maximum number of configurations to return
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 2 x sizeof(uint32_t) + n x sizeof (<config descriptor>)
//  data: status + total number of configurations supported + array of n config descriptors
//==================================================================================================
// command: EFFECT_CMD_GET_FEATURE_CONFIG
//--------------------------------------------------------------------------------------------------
// description:
//  Retrieves current configuration for a given feature.
// The reply status is:
//      - 0 if feature is supported
//      - -ENOSYS if the feature is not supported,
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t)
//  data: effect_feature_e
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(uint32_t) + sizeof (<config descriptor>)
//  data: status + config descriptor
//==================================================================================================
// command: EFFECT_CMD_SET_FEATURE_CONFIG
//--------------------------------------------------------------------------------------------------
// description:
//  Sets current configuration for a given feature.
// The reply status is:
//      - 0 if feature is supported
//      - -ENOSYS if the feature is not supported,
//      - -EINVAL if the configuration is invalid
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t) + sizeof (<config descriptor>)
//  data: effect_feature_e + config descriptor
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(uint32_t)
//  data: status
//==================================================================================================
// command: EFFECT_CMD_SET_AUDIO_SOURCE
//--------------------------------------------------------------------------------------------------
// description:
//  Set the audio source the capture path is configured for (Camcorder, voice recognition...).
//  See audio.h, audio_source_t for values.
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(uint32_t)
//  data: uint32_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: 0
//  data: N/A
//==================================================================================================
// command: EFFECT_CMD_OFFLOAD
//--------------------------------------------------------------------------------------------------
// description:
//  1.indicate if the playback thread the effect is attached to is offloaded or not
//  2.update the io handle of the playback thread the effect is attached to
//--------------------------------------------------------------------------------------------------
// command format:
//  size: sizeof(effect_offload_param_t)
//  data: effect_offload_param_t
//--------------------------------------------------------------------------------------------------
// reply format:
//  size: sizeof(uint32_t)
//  data: uint32_t
//--------------------------------------------------------------------------------------------------
// command: EFFECT_CMD_FIRST_PROPRIETARY
//--------------------------------------------------------------------------------------------------
// description:
//  All proprietary effect commands must use command codes above this value. The size and format of
//  command and response fields is free in this case
//==================================================================================================

// Audio buffer descriptor used by process(), bufferProvider() functions and buffer_config_t
// structure. Multi-channel audio is always interleaved. The channel order is from LSB to MSB with
// regard to the channel mask definition in audio.h, audio_channel_mask_t e.g :
// Stereo: left, right
// 5 point 1: front left, front right, front center, low frequency, back left, back right
// The buffer size is expressed in frame count, a frame being composed of samples for all
// channels at a given time. Frame size for unspecified format (AUDIO_FORMAT_OTHER) is 8 bit by
// definition
typedef struct audio_buffer_s {
    size_t   frameCount;        // number of frames in buffer
    union {
        void*       raw;        // raw pointer to start of buffer
        float*      f32;        // pointer to float 32 bit data at start of buffer
        int32_t*    s32;        // pointer to signed 32 bit data at start of buffer
        int16_t*    s16;        // pointer to signed 16 bit data at start of buffer
        uint8_t*    u8;         // pointer to unsigned 8 bit data at start of buffer
    };
} audio_buffer_t;

typedef int32_t (* buffer_function_t)(void *cookie, audio_buffer_t *buffer);
typedef struct buffer_provider_s
{
    buffer_function_t getBuffer;       // retrieve next buffer
    buffer_function_t releaseBuffer;   // release used buffer
    void       *cookie;                // for use by client of buffer provider functions
} buffer_provider_t;


// The buffer_config_s structure specifies the input or output audio format
// to be used by the effect engine. It is part of the effect_config_t
// structure that defines both input and output buffer configurations and is
// passed by the EFFECT_CMD_SET_CONFIG or EFFECT_CMD_SET_CONFIG_REVERSE command.
typedef struct buffer_config_s
{
    audio_buffer_t  buffer;     // buffer for use by process() function if not passed explicitly
    uint32_t   samplingRate;    // sampling rate
    uint32_t   channels;        // channel mask (see audio_channel_mask_t in audio.h)
    buffer_provider_t bufferProvider;   // buffer provider
    uint8_t    format;          // Audio format (see audio_format_t in audio.h)
    uint8_t    accessMode;      // read/write or accumulate in buffer (effect_buffer_access_e)
    uint16_t   mask;            // indicates which of the above fields is valid
} buffer_config_t;
// Values for "accessMode" field of buffer_config_t:
//   overwrite, read only, accumulate (read/modify/write)
typedef enum
{
    EFFECT_BUFFER_ACCESS_WRITE,
    EFFECT_BUFFER_ACCESS_READ,
    EFFECT_BUFFER_ACCESS_ACCUMULATE
} effect_buffer_access_e;
// effect_config_s structure describes the format of the pCmdData argument of EFFECT_CMD_SET_CONFIG
// command to configure audio parameters and buffers for effect engine input and output.
typedef struct effect_config_s
{
    buffer_config_t   inputCfg;
    buffer_config_t   outputCfg;
} effect_config_t;
// effect_param_s structure describes the format of the pCmdData argument of EFFECT_CMD_SET_PARAM
// command and pCmdData and pReplyData of EFFECT_CMD_GET_PARAM command.
// psize and vsize represent the actual size of parameter and value.
//
// NOTE: the start of value field inside the data field is always on a 32 bit boundary:
//
//  +-----------+
//  | status    | sizeof(int)
//  +-----------+
//  | psize     | sizeof(int)
//  +-----------+
//  | vsize     | sizeof(int)
//  +-----------+
//  |           |   |           |
//  ~ parameter ~   > psize     |
//  |           |   |           >  ((psize - 1)/sizeof(int) + 1) * sizeof(int)
//  +-----------+               |
//  | padding   |               |
//  +-----------+
//  |           |   |
//  ~ value     ~   > vsize
//  |           |   |
//  +-----------+

typedef struct effect_param_s
{
    int32_t     status;     // Transaction status (unused for command, used for reply)
    uint32_t    psize;      // Parameter size
    uint32_t    vsize;      // Value size
    char        data[];     // Start of Parameter + Value data
} effect_param_t;

/////////////////////////////////////////////////
//      Effect library interface
/////////////////////////////////////////////////

// Effect library interface version 3.0
// Note that EffectsFactory.c only checks the major version component, so changes to the minor
// number can only be used for fully backwards compatible changes
#define EFFECT_LIBRARY_API_VERSION EFFECT_MAKE_API_VERSION(3,0)

#define AUDIO_EFFECT_LIBRARY_TAG ((('A') << 24) | (('E') << 16) | (('L') << 8) | ('T'))

// Every effect library must have a data structure named AUDIO_EFFECT_LIBRARY_INFO_SYM
// and the fields of this data structure must begin with audio_effect_library_t

typedef struct audio_effect_library_s
{
    // tag must be initialized to AUDIO_EFFECT_LIBRARY_TAG
    uint32_t tag;
    // Version of the effect library API : 0xMMMMmmmm MMMM: Major, mmmm: minor
    uint32_t version;
    // Name of this library
    const char *name;
    // Author/owner/implementor of the library
    const char *implementor;

    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:        create_effect
    //
    //    Description:    Creates an effect engine of the specified implementation uuid and
    //          returns an effect control interface on this engine. The function will allocate the
    //          resources for an instance of the requested effect engine and return
    //          a handle on the effect control interface.
    //
    //    Input:
    //          uuid:    pointer to the effect uuid.
    //          sessionId:  audio session to which this effect instance will be attached. All effects
    //              created with the same session ID are connected in series and process the same signal
    //              stream. Knowing that two effects are part of the same effect chain can help the
    //              library implement some kind of optimizations.
    //          ioId:   identifies the output or input stream this effect is directed to at audio HAL.
    //              For future use especially with tunneled HW accelerated effects
    //
    //    Input/Output:
    //          pHandle:        address where to return the effect interface handle.
    //
    //    Output:
    //        returned value:    0          successful operation.
    //                          -ENODEV     library failed to initialize
    //                          -EINVAL     invalid pEffectUuid or pHandle
    //                          -ENOENT     no effect with this uuid found
    //        *pHandle:         updated with the effect interface handle.
    //
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*create_effect)(const effect_uuid_t *uuid,
                             int32_t sessionId,
                             int32_t ioId,
                             effect_handle_t *pHandle);

    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:        release_effect
    //
    //    Description:    Releases the effect engine whose handle is given as argument.
    //          All resources allocated to this particular instance of the effect are
    //          released.
    //
    //    Input:
    //          handle:         handle on the effect interface to be released.
    //
    //    Output:
    //        returned value:    0          successful operation.
    //                          -ENODEV     library failed to initialize
    //                          -EINVAL     invalid interface handle
    //
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*release_effect)(effect_handle_t handle);

    ////////////////////////////////////////////////////////////////////////////////
    //
    //    Function:        get_descriptor
    //
    //    Description:    Returns the descriptor of the effect engine which implementation UUID is
    //          given as argument.
    //
    //    Input/Output:
    //          uuid:           pointer to the effect uuid.
    //          pDescriptor:    address where to return the effect descriptor.
    //
    //    Output:
    //        returned value:    0          successful operation.
    //                          -ENODEV     library failed to initialize
    //                          -EINVAL     invalid pDescriptor or uuid
    //        *pDescriptor:     updated with the effect descriptor.
    //
    ////////////////////////////////////////////////////////////////////////////////
    int32_t (*get_descriptor)(const effect_uuid_t *uuid,
                              effect_descriptor_t *pDescriptor);
} audio_effect_library_t;

// Name of the hal_module_info
#define AUDIO_EFFECT_LIBRARY_INFO_SYM         AELI

// Name of the hal_module_info as a string
#define AUDIO_EFFECT_LIBRARY_INFO_SYM_AS_STR  "AELI"

// Values for bit field "mask" in buffer_config_t. If a bit is set, the corresponding field
// in buffer_config_t must be taken into account when executing the EFFECT_CMD_SET_CONFIG command
#define EFFECT_CONFIG_BUFFER    0x0001  // buffer field must be taken into account
#define EFFECT_CONFIG_SMP_RATE  0x0002  // samplingRate field must be taken into account
#define EFFECT_CONFIG_CHANNELS  0x0004  // channels field must be taken into account
#define EFFECT_CONFIG_FORMAT    0x0008  // format field must be taken into account
#define EFFECT_CONFIG_ACC_MODE  0x0010  // accessMode field must be taken into account
#define EFFECT_CONFIG_PROVIDER  0x0020  // bufferProvider field must be taken into account
#define EFFECT_CONFIG_ALL (EFFECT_CONFIG_BUFFER | EFFECT_CONFIG_SMP_RATE | \
                           EFFECT_CONFIG_CHANNELS | EFFECT_CONFIG_FORMAT | \
                           EFFECT_CONFIG_ACC_MODE | EFFECT_CONFIG_PROVIDER)

enum {
    AUDIO_IO_HANDLE_NONE = 0,
    AUDIO_MODULE_HANDLE_NONE = 0,
    AUDIO_PORT_HANDLE_NONE = 0,
    AUDIO_PATCH_HANDLE_NONE = 0,
};

typedef enum {
    AUDIO_STREAM_DEFAULT = -1, // (-1)
    AUDIO_STREAM_MIN = 0,
    AUDIO_STREAM_VOICE_CALL = 0,
    AUDIO_STREAM_SYSTEM = 1,
    AUDIO_STREAM_RING = 2,
    AUDIO_STREAM_MUSIC = 3,
    AUDIO_STREAM_ALARM = 4,
    AUDIO_STREAM_NOTIFICATION = 5,
    AUDIO_STREAM_BLUETOOTH_SCO = 6,
    AUDIO_STREAM_ENFORCED_AUDIBLE = 7,
    AUDIO_STREAM_DTMF = 8,
    AUDIO_STREAM_TTS = 9,
    AUDIO_STREAM_ACCESSIBILITY = 10,
#ifndef AUDIO_NO_SYSTEM_DECLARATIONS
    /** For dynamic policy output mixes. Only used by the audio policy */
    AUDIO_STREAM_REROUTING = 11,
    /** For audio flinger tracks volume. Only used by the audioflinger */
    AUDIO_STREAM_PATCH = 12,
#endif // AUDIO_NO_SYSTEM_DECLARATIONS
} audio_stream_type_t;

typedef enum {
    AUDIO_SOURCE_DEFAULT = 0,
    AUDIO_SOURCE_MIC = 1,
    AUDIO_SOURCE_VOICE_UPLINK = 2,
    AUDIO_SOURCE_VOICE_DOWNLINK = 3,
    AUDIO_SOURCE_VOICE_CALL = 4,
    AUDIO_SOURCE_CAMCORDER = 5,
    AUDIO_SOURCE_VOICE_RECOGNITION = 6,
    AUDIO_SOURCE_VOICE_COMMUNICATION = 7,
    AUDIO_SOURCE_REMOTE_SUBMIX = 8,
    AUDIO_SOURCE_UNPROCESSED = 9,
    AUDIO_SOURCE_FM_TUNER = 1998,
#ifndef AUDIO_NO_SYSTEM_DECLARATIONS
    /**
     * A low-priority, preemptible audio source for for background software
     * hotword detection. Same tuning as VOICE_RECOGNITION.
     * Used only internally by the framework.
     */
    AUDIO_SOURCE_HOTWORD = 1999,
#endif // AUDIO_NO_SYSTEM_DECLARATIONS
} audio_source_t;

typedef enum {
    AUDIO_SESSION_OUTPUT_STAGE = -1, // (-1)
    AUDIO_SESSION_OUTPUT_MIX = 0,
    AUDIO_SESSION_ALLOCATE = 0,
    AUDIO_SESSION_NONE = 0,
} audio_session_t;

typedef enum {
    AUDIO_FORMAT_INVALID             = 0xFFFFFFFFu,
    AUDIO_FORMAT_DEFAULT             = 0,
    AUDIO_FORMAT_PCM                 = 0x00000000u,
    AUDIO_FORMAT_MP3                 = 0x01000000u,
    AUDIO_FORMAT_AMR_NB              = 0x02000000u,
    AUDIO_FORMAT_AMR_WB              = 0x03000000u,
    AUDIO_FORMAT_AAC                 = 0x04000000u,
    AUDIO_FORMAT_HE_AAC_V1           = 0x05000000u,
    AUDIO_FORMAT_HE_AAC_V2           = 0x06000000u,
    AUDIO_FORMAT_VORBIS              = 0x07000000u,
    AUDIO_FORMAT_OPUS                = 0x08000000u,
    AUDIO_FORMAT_AC3                 = 0x09000000u,
    AUDIO_FORMAT_E_AC3               = 0x0A000000u,
    AUDIO_FORMAT_DTS                 = 0x0B000000u,
    AUDIO_FORMAT_DTS_HD              = 0x0C000000u,
    AUDIO_FORMAT_IEC61937            = 0x0D000000u,
    AUDIO_FORMAT_DOLBY_TRUEHD        = 0x0E000000u,
    AUDIO_FORMAT_EVRC                = 0x10000000u,
    AUDIO_FORMAT_EVRCB               = 0x11000000u,
    AUDIO_FORMAT_EVRCWB              = 0x12000000u,
    AUDIO_FORMAT_EVRCNW              = 0x13000000u,
    AUDIO_FORMAT_AAC_ADIF            = 0x14000000u,
    AUDIO_FORMAT_WMA                 = 0x15000000u,
    AUDIO_FORMAT_WMA_PRO             = 0x16000000u,
    AUDIO_FORMAT_AMR_WB_PLUS         = 0x17000000u,
    AUDIO_FORMAT_MP2                 = 0x18000000u,
    AUDIO_FORMAT_QCELP               = 0x19000000u,
    AUDIO_FORMAT_DSD                 = 0x1A000000u,
    AUDIO_FORMAT_FLAC                = 0x1B000000u,
    AUDIO_FORMAT_ALAC                = 0x1C000000u,
    AUDIO_FORMAT_APE                 = 0x1D000000u,
    AUDIO_FORMAT_AAC_ADTS            = 0x1E000000u,
    AUDIO_FORMAT_SBC                 = 0x1F000000u,
    AUDIO_FORMAT_APTX                = 0x20000000u,
    AUDIO_FORMAT_APTX_HD             = 0x21000000u,
    AUDIO_FORMAT_AC4                 = 0x22000000u,
    AUDIO_FORMAT_LDAC                = 0x23000000u,
    AUDIO_FORMAT_MAT                 = 0x24000000u,
    AUDIO_FORMAT_MAIN_MASK           = 0xFF000000u,
    AUDIO_FORMAT_SUB_MASK            = 0x00FFFFFFu,

    /* Subformats */
    AUDIO_FORMAT_PCM_SUB_16_BIT        = 0x1u,
    AUDIO_FORMAT_PCM_SUB_8_BIT         = 0x2u,
    AUDIO_FORMAT_PCM_SUB_32_BIT        = 0x3u,
    AUDIO_FORMAT_PCM_SUB_8_24_BIT      = 0x4u,
    AUDIO_FORMAT_PCM_SUB_FLOAT         = 0x5u,
    AUDIO_FORMAT_PCM_SUB_24_BIT_PACKED = 0x6u,

    AUDIO_FORMAT_MP3_SUB_NONE          = 0x0u,

    AUDIO_FORMAT_AMR_SUB_NONE          = 0x0u,

    AUDIO_FORMAT_AAC_SUB_MAIN          = 0x1u,
    AUDIO_FORMAT_AAC_SUB_LC            = 0x2u,
    AUDIO_FORMAT_AAC_SUB_SSR           = 0x4u,
    AUDIO_FORMAT_AAC_SUB_LTP           = 0x8u,
    AUDIO_FORMAT_AAC_SUB_HE_V1         = 0x10u,
    AUDIO_FORMAT_AAC_SUB_SCALABLE      = 0x20u,
    AUDIO_FORMAT_AAC_SUB_ERLC          = 0x40u,
    AUDIO_FORMAT_AAC_SUB_LD            = 0x80u,
    AUDIO_FORMAT_AAC_SUB_HE_V2         = 0x100u,
    AUDIO_FORMAT_AAC_SUB_ELD           = 0x200u,
    AUDIO_FORMAT_AAC_SUB_XHE           = 0x300u,

    AUDIO_FORMAT_VORBIS_SUB_NONE       = 0x0u,

    AUDIO_FORMAT_E_AC3_SUB_JOC         = 0x1u,

    AUDIO_FORMAT_MAT_SUB_1_0           = 0x1u,
    AUDIO_FORMAT_MAT_SUB_2_0           = 0x2u,
    AUDIO_FORMAT_MAT_SUB_2_1           = 0x3u,

    /* Aliases */
    AUDIO_FORMAT_PCM_16_BIT            = 0x1u,        // (PCM | PCM_SUB_16_BIT)
    AUDIO_FORMAT_PCM_8_BIT             = 0x2u,        // (PCM | PCM_SUB_8_BIT)
    AUDIO_FORMAT_PCM_32_BIT            = 0x3u,        // (PCM | PCM_SUB_32_BIT)
    AUDIO_FORMAT_PCM_8_24_BIT          = 0x4u,        // (PCM | PCM_SUB_8_24_BIT)
    AUDIO_FORMAT_PCM_FLOAT             = 0x5u,        // (PCM | PCM_SUB_FLOAT)
    AUDIO_FORMAT_PCM_24_BIT_PACKED     = 0x6u,        // (PCM | PCM_SUB_24_BIT_PACKED)
    AUDIO_FORMAT_AAC_MAIN              = 0x4000001u,  // (AAC | AAC_SUB_MAIN)
    AUDIO_FORMAT_AAC_LC                = 0x4000002u,  // (AAC | AAC_SUB_LC)
    AUDIO_FORMAT_AAC_SSR               = 0x4000004u,  // (AAC | AAC_SUB_SSR)
    AUDIO_FORMAT_AAC_LTP               = 0x4000008u,  // (AAC | AAC_SUB_LTP)
    AUDIO_FORMAT_AAC_HE_V1             = 0x4000010u,  // (AAC | AAC_SUB_HE_V1)
    AUDIO_FORMAT_AAC_SCALABLE          = 0x4000020u,  // (AAC | AAC_SUB_SCALABLE)
    AUDIO_FORMAT_AAC_ERLC              = 0x4000040u,  // (AAC | AAC_SUB_ERLC)
    AUDIO_FORMAT_AAC_LD                = 0x4000080u,  // (AAC | AAC_SUB_LD)
    AUDIO_FORMAT_AAC_HE_V2             = 0x4000100u,  // (AAC | AAC_SUB_HE_V2)
    AUDIO_FORMAT_AAC_ELD               = 0x4000200u,  // (AAC | AAC_SUB_ELD)
    AUDIO_FORMAT_AAC_XHE               = 0x4000300u,  // (AAC | AAC_SUB_XHE)
    AUDIO_FORMAT_AAC_ADTS_MAIN         = 0x1e000001u, // (AAC_ADTS | AAC_SUB_MAIN)
    AUDIO_FORMAT_AAC_ADTS_LC           = 0x1e000002u, // (AAC_ADTS | AAC_SUB_LC)
    AUDIO_FORMAT_AAC_ADTS_SSR          = 0x1e000004u, // (AAC_ADTS | AAC_SUB_SSR)
    AUDIO_FORMAT_AAC_ADTS_LTP          = 0x1e000008u, // (AAC_ADTS | AAC_SUB_LTP)
    AUDIO_FORMAT_AAC_ADTS_HE_V1        = 0x1e000010u, // (AAC_ADTS | AAC_SUB_HE_V1)
    AUDIO_FORMAT_AAC_ADTS_SCALABLE     = 0x1e000020u, // (AAC_ADTS | AAC_SUB_SCALABLE)
    AUDIO_FORMAT_AAC_ADTS_ERLC         = 0x1e000040u, // (AAC_ADTS | AAC_SUB_ERLC)
    AUDIO_FORMAT_AAC_ADTS_LD           = 0x1e000080u, // (AAC_ADTS | AAC_SUB_LD)
    AUDIO_FORMAT_AAC_ADTS_HE_V2        = 0x1e000100u, // (AAC_ADTS | AAC_SUB_HE_V2)
    AUDIO_FORMAT_AAC_ADTS_ELD          = 0x1e000200u, // (AAC_ADTS | AAC_SUB_ELD)
    AUDIO_FORMAT_AAC_ADTS_XHE          = 0x1e000300u, // (AAC_ADTS | AAC_SUB_XHE)
    AUDIO_FORMAT_E_AC3_JOC             = 0xA000001u,  // (E_AC3 | E_AC3_SUB_JOC)
    AUDIO_FORMAT_MAT_1_0               = 0x24000001u, // (MAT | MAT_SUB_1_0)
    AUDIO_FORMAT_MAT_2_0               = 0x24000002u, // (MAT | MAT_SUB_2_0)
    AUDIO_FORMAT_MAT_2_1               = 0x24000003u, // (MAT | MAT_SUB_2_1)
} audio_format_t;

enum {
    FCC_2 = 2,
    FCC_8 = 8,
};

enum {
    AUDIO_CHANNEL_REPRESENTATION_POSITION   = 0x0u,
    AUDIO_CHANNEL_REPRESENTATION_INDEX      = 0x2u,
    AUDIO_CHANNEL_NONE                      = 0x0u,
    AUDIO_CHANNEL_INVALID                   = 0xC0000000u,

    AUDIO_CHANNEL_OUT_FRONT_LEFT            = 0x1u,
    AUDIO_CHANNEL_OUT_FRONT_RIGHT           = 0x2u,
    AUDIO_CHANNEL_OUT_FRONT_CENTER          = 0x4u,
    AUDIO_CHANNEL_OUT_LOW_FREQUENCY         = 0x8u,
    AUDIO_CHANNEL_OUT_BACK_LEFT             = 0x10u,
    AUDIO_CHANNEL_OUT_BACK_RIGHT            = 0x20u,
    AUDIO_CHANNEL_OUT_FRONT_LEFT_OF_CENTER  = 0x40u,
    AUDIO_CHANNEL_OUT_FRONT_RIGHT_OF_CENTER = 0x80u,
    AUDIO_CHANNEL_OUT_BACK_CENTER           = 0x100u,
    AUDIO_CHANNEL_OUT_SIDE_LEFT             = 0x200u,
    AUDIO_CHANNEL_OUT_SIDE_RIGHT            = 0x400u,
    AUDIO_CHANNEL_OUT_TOP_CENTER            = 0x800u,
    AUDIO_CHANNEL_OUT_TOP_FRONT_LEFT        = 0x1000u,
    AUDIO_CHANNEL_OUT_TOP_FRONT_CENTER      = 0x2000u,
    AUDIO_CHANNEL_OUT_TOP_FRONT_RIGHT       = 0x4000u,
    AUDIO_CHANNEL_OUT_TOP_BACK_LEFT         = 0x8000u,
    AUDIO_CHANNEL_OUT_TOP_BACK_CENTER       = 0x10000u,
    AUDIO_CHANNEL_OUT_TOP_BACK_RIGHT        = 0x20000u,
    AUDIO_CHANNEL_OUT_TOP_SIDE_LEFT         = 0x40000u,
    AUDIO_CHANNEL_OUT_TOP_SIDE_RIGHT        = 0x80000u,
    AUDIO_CHANNEL_OUT_MONO                  = 0x1u,     // OUT_FRONT_LEFT
    AUDIO_CHANNEL_OUT_STEREO                = 0x3u,     // OUT_FRONT_LEFT | OUT_FRONT_RIGHT
    AUDIO_CHANNEL_OUT_2POINT1               = 0xBu,     // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_LOW_FREQUENCY
    AUDIO_CHANNEL_OUT_2POINT0POINT2         = 0xC0003u, // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_2POINT1POINT2         = 0xC000Bu, // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT | OUT_LOW_FREQUENCY
    AUDIO_CHANNEL_OUT_3POINT0POINT2         = 0xC0007u, // OUT_FRONT_LEFT | OUT_FRONT_CENTER | OUT_FRONT_RIGHT | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_3POINT1POINT2         = 0xC000Fu, // OUT_FRONT_LEFT | OUT_FRONT_CENTER | OUT_FRONT_RIGHT | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT | OUT_LOW_FREQUENCY
    AUDIO_CHANNEL_OUT_QUAD                  = 0x33u,    // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_BACK_LEFT | OUT_BACK_RIGHT
    AUDIO_CHANNEL_OUT_QUAD_BACK             = 0x33u,    // OUT_QUAD
    AUDIO_CHANNEL_OUT_QUAD_SIDE             = 0x603u,   // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_SIDE_LEFT | OUT_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_SURROUND              = 0x107u,   // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_FRONT_CENTER | OUT_BACK_CENTER
    AUDIO_CHANNEL_OUT_PENTA                 = 0x37u,    // OUT_QUAD | OUT_FRONT_CENTER
    AUDIO_CHANNEL_OUT_5POINT1               = 0x3Fu,    // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_FRONT_CENTER | OUT_LOW_FREQUENCY | OUT_BACK_LEFT | OUT_BACK_RIGHT
    AUDIO_CHANNEL_OUT_5POINT1_BACK          = 0x3Fu,    // OUT_5POINT1
    AUDIO_CHANNEL_OUT_5POINT1_SIDE          = 0x60Fu,   // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_FRONT_CENTER | OUT_LOW_FREQUENCY | OUT_SIDE_LEFT | OUT_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_5POINT1POINT2         = 0xC003Fu, // OUT_5POINT1 | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_5POINT1POINT4         = 0x2D03Fu, // OUT_5POINT1 | OUT_TOP_FRONT_LEFT | OUT_TOP_FRONT_RIGHT | OUT_TOP_BACK_LEFT | OUT_TOP_BACK_RIGHT
    AUDIO_CHANNEL_OUT_6POINT1               = 0x13Fu,   // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_FRONT_CENTER | OUT_LOW_FREQUENCY | OUT_BACK_LEFT | OUT_BACK_RIGHT | OUT_BACK_CENTER
    AUDIO_CHANNEL_OUT_7POINT1               = 0x63Fu,   // OUT_FRONT_LEFT | OUT_FRONT_RIGHT | OUT_FRONT_CENTER | OUT_LOW_FREQUENCY | OUT_BACK_LEFT | OUT_BACK_RIGHT | OUT_SIDE_LEFT | OUT_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_7POINT1POINT2         = 0xC063Fu, // OUT_7POINT1 | OUT_TOP_SIDE_LEFT | OUT_TOP_SIDE_RIGHT
    AUDIO_CHANNEL_OUT_7POINT1POINT4         = 0x2D63Fu, // OUT_7POINT1 | OUT_TOP_FRONT_LEFT | OUT_TOP_FRONT_RIGHT | OUT_TOP_BACK_LEFT | OUT_TOP_BACK_RIGHT

    AUDIO_CHANNEL_IN_LEFT                   = 0x4u,
    AUDIO_CHANNEL_IN_RIGHT                  = 0x8u,
    AUDIO_CHANNEL_IN_FRONT                  = 0x10u,
    AUDIO_CHANNEL_IN_BACK                   = 0x20u,
    AUDIO_CHANNEL_IN_LEFT_PROCESSED         = 0x40u,
    AUDIO_CHANNEL_IN_RIGHT_PROCESSED        = 0x80u,
    AUDIO_CHANNEL_IN_FRONT_PROCESSED        = 0x100u,
    AUDIO_CHANNEL_IN_BACK_PROCESSED         = 0x200u,
    AUDIO_CHANNEL_IN_PRESSURE               = 0x400u,
    AUDIO_CHANNEL_IN_X_AXIS                 = 0x800u,
    AUDIO_CHANNEL_IN_Y_AXIS                 = 0x1000u,
    AUDIO_CHANNEL_IN_Z_AXIS                 = 0x2000u,
    AUDIO_CHANNEL_IN_BACK_LEFT              = 0x10000u,
    AUDIO_CHANNEL_IN_BACK_RIGHT             = 0x20000u,
    AUDIO_CHANNEL_IN_CENTER                 = 0x40000u,
    AUDIO_CHANNEL_IN_LOW_FREQUENCY          = 0x100000u,
    AUDIO_CHANNEL_IN_TOP_LEFT               = 0x200000u,
    AUDIO_CHANNEL_IN_TOP_RIGHT              = 0x400000u,
    AUDIO_CHANNEL_IN_VOICE_UPLINK           = 0x4000u,
    AUDIO_CHANNEL_IN_VOICE_DNLINK           = 0x8000u,
    AUDIO_CHANNEL_IN_MONO                   = 0x10u,     // IN_FRONT
    AUDIO_CHANNEL_IN_STEREO                 = 0xCu,      // IN_LEFT | IN_RIGHT
    AUDIO_CHANNEL_IN_FRONT_BACK             = 0x30u,     // IN_FRONT | IN_BACK
    AUDIO_CHANNEL_IN_6                      = 0xFCu,     // IN_LEFT | IN_RIGHT | IN_FRONT | IN_BACK | IN_LEFT_PROCESSED | IN_RIGHT_PROCESSED
    AUDIO_CHANNEL_IN_2POINT0POINT2          = 0x60000Cu, // IN_LEFT | IN_RIGHT | IN_TOP_LEFT | IN_TOP_RIGHT
    AUDIO_CHANNEL_IN_2POINT1POINT2          = 0x70000Cu, // IN_LEFT | IN_RIGHT | IN_TOP_LEFT | IN_TOP_RIGHT | IN_LOW_FREQUENCY
    AUDIO_CHANNEL_IN_3POINT0POINT2          = 0x64000Cu, // IN_LEFT | IN_CENTER | IN_RIGHT | IN_TOP_LEFT | IN_TOP_RIGHT
    AUDIO_CHANNEL_IN_3POINT1POINT2          = 0x74000Cu, // IN_LEFT | IN_CENTER | IN_RIGHT | IN_TOP_LEFT | IN_TOP_RIGHT | IN_LOW_FREQUENCY
    AUDIO_CHANNEL_IN_5POINT1                = 0x17000Cu, // IN_LEFT | IN_CENTER | IN_RIGHT | IN_BACK_LEFT | IN_BACK_RIGHT | IN_LOW_FREQUENCY
    AUDIO_CHANNEL_IN_VOICE_UPLINK_MONO      = 0x4010u,   // IN_VOICE_UPLINK | IN_MONO
    AUDIO_CHANNEL_IN_VOICE_DNLINK_MONO      = 0x8010u,   // IN_VOICE_DNLINK | IN_MONO
    AUDIO_CHANNEL_IN_VOICE_CALL_MONO        = 0xC010u,   // IN_VOICE_UPLINK_MONO | IN_VOICE_DNLINK_MONO

    AUDIO_CHANNEL_COUNT_MAX                 = 30u,
    AUDIO_CHANNEL_INDEX_HDR                 = 0x80000000u, // REPRESENTATION_INDEX << COUNT_MAX
    AUDIO_CHANNEL_INDEX_MASK_1              = 0x80000001u, // INDEX_HDR | (1 << 1) - 1
    AUDIO_CHANNEL_INDEX_MASK_2              = 0x80000003u, // INDEX_HDR | (1 << 2) - 1
    AUDIO_CHANNEL_INDEX_MASK_3              = 0x80000007u, // INDEX_HDR | (1 << 3) - 1
    AUDIO_CHANNEL_INDEX_MASK_4              = 0x8000000Fu, // INDEX_HDR | (1 << 4) - 1
    AUDIO_CHANNEL_INDEX_MASK_5              = 0x8000001Fu, // INDEX_HDR | (1 << 5) - 1
    AUDIO_CHANNEL_INDEX_MASK_6              = 0x8000003Fu, // INDEX_HDR | (1 << 6) - 1
    AUDIO_CHANNEL_INDEX_MASK_7              = 0x8000007Fu, // INDEX_HDR | (1 << 7) - 1
    AUDIO_CHANNEL_INDEX_MASK_8              = 0x800000FFu, // INDEX_HDR | (1 << 8) - 1
};
#endif