#include "TwoStageFFTConvolver.h"
#include <stdlib.h>
#include <string.h>
#include <float.h>
#include <math.h>
#include "../jdsp_header.h"
void TwoStageFFTConvolver1x1Init(TwoStageFFTConvolver1x1 *conv)
{
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	FFTConvolver1x1Init(&conv->_headConvolver);
	FFTConvolver1x1Init(&conv->_tailConvolver0);
	FFTConvolver1x1Init(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	conv->_tailOutput0 = 0;
	conv->_tailPrecalculated0 = 0;
	conv->_tailOutput = 0;
	conv->_tailInput = 0;
	conv->_tailPrecalculated = 0;
	conv->_backgroundProcessingInput = 0;
#ifdef THREAD
	conv->shared_info.state = NOTHING;
#endif
}
void TwoStageFFTConvolver2x4x2Init(TwoStageFFTConvolver2x4x2 *conv)
{
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	FFTConvolver2x4x2Init(&conv->_headConvolver);
	FFTConvolver2x4x2Init(&conv->_tailConvolver0);
	FFTConvolver2x4x2Init(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	conv->_tailOutput0[0] = 0;
	conv->_tailPrecalculated0[0] = 0;
	conv->_tailOutput[0] = 0;
	conv->_tailInput[0] = 0;
	conv->_tailPrecalculated[0] = 0;
	conv->_backgroundProcessingInput[0] = 0;
#ifdef THREAD
	conv->shared_info.state = NOTHING;
#endif
}
void TwoStageFFTConvolver2x2Init(TwoStageFFTConvolver2x2 *conv)
{
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	FFTConvolver2x2Init(&conv->_headConvolver);
	FFTConvolver2x2Init(&conv->_tailConvolver0);
	FFTConvolver2x2Init(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	conv->_tailOutput0[0] = 0;
	conv->_tailPrecalculated0[0] = 0;
	conv->_tailOutput[0] = 0;
	conv->_tailInput[0] = 0;
	conv->_tailPrecalculated[0] = 0;
	conv->_backgroundProcessingInput[0] = 0;
#ifdef THREAD
	conv->shared_info.state = NOTHING;
#endif
}
void TwoStageFFTConvolver1x2Init(TwoStageFFTConvolver1x2 *conv)
{
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	FFTConvolver1x2Init(&conv->_headConvolver);
	FFTConvolver1x2Init(&conv->_tailConvolver0);
	FFTConvolver1x2Init(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	conv->_tailOutput0[0] = 0;
	conv->_tailPrecalculated0[0] = 0;
	conv->_tailOutput[0] = 0;
	conv->_tailInput = 0;
	conv->_tailPrecalculated[0] = 0;
	conv->_backgroundProcessingInput = 0;
#ifdef THREAD
	conv->shared_info.state = NOTHING;
#endif
}

#ifdef THREAD
inline void task_wait1x1(TwoStageFFTConvolver1x1 *conv)
{
	pt_info *info = &conv->shared_info;
	while (1)
	{
		pthread_cond_wait(&(info->boss_cond), &(info->boss_mtx));
		if (IDLE == info->state)
			break;
	}
}
inline void task_wait2x4x2(TwoStageFFTConvolver2x4x2 *conv)
{
	pt_info2x4x2 *info = &conv->shared_info;
	while (1)
	{
		pthread_cond_wait(&(info->boss_cond), &(info->boss_mtx));
		if (IDLE == info->state)
			break;
	}
}
inline void task_wait2x2(TwoStageFFTConvolver2x2 *conv)
{
	pt_info2x2 *info = &conv->shared_info;
	while (1)
	{
		pthread_cond_wait(&(info->boss_cond), &(info->boss_mtx));
		if (IDLE == info->state)
			break;
	}
}
inline void task_wait1x2(TwoStageFFTConvolver1x2 *conv)
{
	pt_info1x2 *info = &conv->shared_info;
	while (1)
	{
		pthread_cond_wait(&(info->boss_cond), &(info->boss_mtx));
		if (IDLE == info->state)
			break;
	}
}
void thread_exit1x1(TwoStageFFTConvolver1x1 *conv)
{
	pt_info *info = NULL;
	//		GFT->sa_log_d(TAG, "Thread %d get off from work", i + 1);
	info = &conv->shared_info;
	// ensure the worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	info->state = GET_OFF_FROM_WORK;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
	// wait for thread to exit
	pthread_join(conv->threads, NULL);
	pthread_mutex_destroy(&(info->work_mtx));
	pthread_cond_destroy(&(info->work_cond));
	pthread_mutex_unlock(&(info->boss_mtx));
	pthread_mutex_destroy(&(info->boss_mtx));
	pthread_cond_destroy(&(info->boss_cond));
}
void thread_exit2x4x2(TwoStageFFTConvolver2x4x2 *conv)
{
	pt_info2x4x2 *info = NULL;
	//		GFT->sa_log_d(TAG, "Thread %d get off from work", i + 1);
	info = &conv->shared_info;
	// ensure the worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	info->state = GET_OFF_FROM_WORK;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
	// wait for thread to exit
	pthread_join(conv->threads, NULL);
	pthread_mutex_destroy(&(info->work_mtx));
	pthread_cond_destroy(&(info->work_cond));
	pthread_mutex_unlock(&(info->boss_mtx));
	pthread_mutex_destroy(&(info->boss_mtx));
	pthread_cond_destroy(&(info->boss_cond));
}
void thread_exit2x2(TwoStageFFTConvolver2x2 *conv)
{
	pt_info2x2 *info = NULL;
	//		GFT->sa_log_d(TAG, "Thread %d get off from work", i + 1);
	info = &conv->shared_info;
	// ensure the worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	info->state = GET_OFF_FROM_WORK;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
	// wait for thread to exit
	pthread_join(conv->threads, NULL);
	pthread_mutex_destroy(&(info->work_mtx));
	pthread_cond_destroy(&(info->work_cond));
	pthread_mutex_unlock(&(info->boss_mtx));
	pthread_mutex_destroy(&(info->boss_mtx));
	pthread_cond_destroy(&(info->boss_cond));
}
void thread_exit1x2(TwoStageFFTConvolver1x2 *conv)
{
	pt_info1x2 *info = NULL;
	//		GFT->sa_log_d(TAG, "Thread %d get off from work", i + 1);
	info = &conv->shared_info;
	// ensure the worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	info->state = GET_OFF_FROM_WORK;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
	// wait for thread to exit
	pthread_join(conv->threads, NULL);
	pthread_mutex_destroy(&(info->work_mtx));
	pthread_cond_destroy(&(info->work_cond));
	pthread_mutex_unlock(&(info->boss_mtx));
	pthread_mutex_destroy(&(info->boss_mtx));
	pthread_cond_destroy(&(info->boss_cond));
}
/**
* @brief Method called by the convolver if work for background processing is available
*
* The default implementation just calls doBackgroundProcessing() to perform the "bulk"
* convolution. However, if you want to perform the majority of work in some background
* thread (which is recommended), you can overload this method and trigger the execution
* of doBackgroundProcessing() really in some background thread.
*/
void *task_type2(void *arg)
{
	pt_info *conv = (pt_info *)arg;
	// cond_wait mutex must be locked before we can wait
	pthread_mutex_lock(&(conv->work_mtx));
	//	printf("<worker %i> start\n", task);
		// ensure boss is waiting
	pthread_mutex_lock(&(conv->boss_mtx));
	// signal to boss that setup is complete
	conv->state = IDLE;
	// wake-up signal
	pthread_cond_signal(&(conv->boss_cond));
	pthread_mutex_unlock(&(conv->boss_mtx));
	while (1)
	{
		pthread_cond_wait(&(conv->work_cond), &(conv->work_mtx));
		if (GET_OFF_FROM_WORK == conv->state)
			break; // kill thread
		if (IDLE == conv->state)
			continue; // accidental wake-up
		// do blocking task
		FFTConvolver1x1Process(conv->_tailConvolver, conv->_backgroundProcessingInput, conv->_tailOutput, conv->_tailBlockSize);
		// ensure boss is waiting
		pthread_mutex_lock(&(conv->boss_mtx));
		// indicate that job is done
		conv->state = IDLE;
		// wake-up signal
		pthread_cond_signal(&(conv->boss_cond));
		pthread_mutex_unlock(&(conv->boss_mtx));
	}
	pthread_mutex_unlock(&(conv->work_mtx));
	pthread_exit(NULL);
	return 0;
}
void *task_type22x4x2(void *arg)
{
	pt_info2x4x2 *conv = (pt_info2x4x2*)arg;
	// cond_wait mutex must be locked before we can wait
	pthread_mutex_lock(&(conv->work_mtx));
	//	printf("<worker %i> start\n", task);
		// ensure boss is waiting
	pthread_mutex_lock(&(conv->boss_mtx));
	// signal to boss that setup is complete
	conv->state = IDLE;
	// wake-up signal
	pthread_cond_signal(&(conv->boss_cond));
	pthread_mutex_unlock(&(conv->boss_mtx));
	while (1)
	{
		pthread_cond_wait(&(conv->work_cond), &(conv->work_mtx));
		if (GET_OFF_FROM_WORK == conv->state)
			break; // kill thread
		if (IDLE == conv->state)
			continue; // accidental wake-up
		// do blocking task
		FFTConvolver2x4x2Process(conv->_tailConvolver, conv->_backgroundProcessingInput[0], conv->_backgroundProcessingInput[1], conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
		// ensure boss is waiting
		pthread_mutex_lock(&(conv->boss_mtx));
		// indicate that job is done
		conv->state = IDLE;
		// wake-up signal
		pthread_cond_signal(&(conv->boss_cond));
		pthread_mutex_unlock(&(conv->boss_mtx));
	}
	pthread_mutex_unlock(&(conv->work_mtx));
	pthread_exit(NULL);
	return 0;
}
void *task_type22x2(void *arg)
{
	pt_info2x2 *conv = (pt_info2x2*)arg;
	// cond_wait mutex must be locked before we can wait
	pthread_mutex_lock(&(conv->work_mtx));
	//	printf("<worker %i> start\n", task);
		// ensure boss is waiting
	pthread_mutex_lock(&(conv->boss_mtx));
	// signal to boss that setup is complete
	conv->state = IDLE;
	// wake-up signal
	pthread_cond_signal(&(conv->boss_cond));
	pthread_mutex_unlock(&(conv->boss_mtx));
	while (1)
	{
		pthread_cond_wait(&(conv->work_cond), &(conv->work_mtx));
		if (GET_OFF_FROM_WORK == conv->state)
			break; // kill thread
		if (IDLE == conv->state)
			continue; // accidental wake-up
		// do blocking task
		FFTConvolver2x2Process(conv->_tailConvolver, conv->_backgroundProcessingInput[0], conv->_backgroundProcessingInput[1], conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
		// ensure boss is waiting
		pthread_mutex_lock(&(conv->boss_mtx));
		// indicate that job is done
		conv->state = IDLE;
		// wake-up signal
		pthread_cond_signal(&(conv->boss_cond));
		pthread_mutex_unlock(&(conv->boss_mtx));
	}
	pthread_mutex_unlock(&(conv->work_mtx));
	pthread_exit(NULL);
	return 0;
}
void *task_type21x2(void *arg)
{
	pt_info1x2 *conv = (pt_info1x2*)arg;
	// cond_wait mutex must be locked before we can wait
	pthread_mutex_lock(&(conv->work_mtx));
	//	printf("<worker %i> start\n", task);
		// ensure boss is waiting
	pthread_mutex_lock(&(conv->boss_mtx));
	// signal to boss that setup is complete
	conv->state = IDLE;
	// wake-up signal
	pthread_cond_signal(&(conv->boss_cond));
	pthread_mutex_unlock(&(conv->boss_mtx));
	while (1)
	{
		pthread_cond_wait(&(conv->work_cond), &(conv->work_mtx));
		if (GET_OFF_FROM_WORK == conv->state)
			break; // kill thread
		if (IDLE == conv->state)
			continue; // accidental wake-up
		// do blocking task
		FFTConvolver1x2Process(conv->_tailConvolver, conv->_backgroundProcessingInput, conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
		// ensure boss is waiting
		pthread_mutex_lock(&(conv->boss_mtx));
		// indicate that job is done
		conv->state = IDLE;
		// wake-up signal
		pthread_cond_signal(&(conv->boss_cond));
		pthread_mutex_unlock(&(conv->boss_mtx));
	}
	pthread_mutex_unlock(&(conv->work_mtx));
	pthread_exit(NULL);
	return 0;
}
void thread_init1x1(TwoStageFFTConvolver1x1 *conv)
{
	pt_info *info = NULL;
	info = &conv->shared_info;
	info->state = SETUP;
	pthread_cond_init(&(info->work_cond), NULL);
	pthread_mutex_init(&(info->work_mtx), NULL);
	pthread_cond_init(&(info->boss_cond), NULL);
	pthread_mutex_init(&(info->boss_mtx), NULL);
	pthread_mutex_lock(&(info->boss_mtx));
	info->_tailConvolver = &conv->_tailConvolver;
	info->_backgroundProcessingInput = conv->_backgroundProcessingInput;
	info->_tailOutput = conv->_tailOutput;
	info->_tailBlockSize = conv->_tailBlockSize;
	pthread_create(&conv->threads, NULL, task_type2, (void *)info);
	task_wait1x1(conv);
}
void thread_init2x4x2(TwoStageFFTConvolver2x4x2 *conv)
{
	pt_info2x4x2 *info = NULL;
	info = &conv->shared_info;
	info->state = SETUP;
	pthread_cond_init(&(info->work_cond), NULL);
	pthread_mutex_init(&(info->work_mtx), NULL);
	pthread_cond_init(&(info->boss_cond), NULL);
	pthread_mutex_init(&(info->boss_mtx), NULL);
	pthread_mutex_lock(&(info->boss_mtx));
	info->_tailConvolver = &conv->_tailConvolver;
	info->_backgroundProcessingInput[0] = conv->_backgroundProcessingInput[0];
	info->_backgroundProcessingInput[1] = conv->_backgroundProcessingInput[1];
	info->_tailOutput[0] = conv->_tailOutput[0];
	info->_tailOutput[1] = conv->_tailOutput[1];
	info->_tailBlockSize = conv->_tailBlockSize;
	pthread_create(&conv->threads, NULL, task_type22x4x2, (void *)info);
	task_wait2x4x2(conv);
}
void thread_init2x2(TwoStageFFTConvolver2x2 *conv)
{
	pt_info2x2 *info = NULL;
	info = &conv->shared_info;
	info->state = SETUP;
	pthread_cond_init(&(info->work_cond), NULL);
	pthread_mutex_init(&(info->work_mtx), NULL);
	pthread_cond_init(&(info->boss_cond), NULL);
	pthread_mutex_init(&(info->boss_mtx), NULL);
	pthread_mutex_lock(&(info->boss_mtx));
	info->_tailConvolver = &conv->_tailConvolver;
	info->_backgroundProcessingInput[0] = conv->_backgroundProcessingInput[0];
	info->_backgroundProcessingInput[1] = conv->_backgroundProcessingInput[1];
	info->_tailOutput[0] = conv->_tailOutput[0];
	info->_tailOutput[1] = conv->_tailOutput[1];
	info->_tailBlockSize = conv->_tailBlockSize;
	pthread_create(&conv->threads, NULL, task_type22x2, (void *)info);
	task_wait2x2(conv);
}
void thread_init1x2(TwoStageFFTConvolver1x2 *conv)
{
	pt_info1x2 *info = NULL;
	info = &conv->shared_info;
	info->state = SETUP;
	pthread_cond_init(&(info->work_cond), NULL);
	pthread_mutex_init(&(info->work_mtx), NULL);
	pthread_cond_init(&(info->boss_cond), NULL);
	pthread_mutex_init(&(info->boss_mtx), NULL);
	pthread_mutex_lock(&(info->boss_mtx));
	info->_tailConvolver = &conv->_tailConvolver;
	info->_backgroundProcessingInput = conv->_backgroundProcessingInput;
	info->_tailOutput[0] = conv->_tailOutput[0];
	info->_tailOutput[1] = conv->_tailOutput[1];
	info->_tailBlockSize = conv->_tailBlockSize;
	pthread_create(&conv->threads, NULL, task_type21x2, (void *)info);
	task_wait1x2(conv);
}
inline void task_start1x1(TwoStageFFTConvolver1x1 *conv)
{
	pt_info *info = &(conv->shared_info);
	// ensure worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	// set job information & state
	info->state = WORKING;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
}
inline void task_start2x4x2(TwoStageFFTConvolver2x4x2 *conv)
{
	pt_info2x4x2 *info = &(conv->shared_info);
	// ensure worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	// set job information & state
	info->state = WORKING;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
}
inline void task_start2x2(TwoStageFFTConvolver2x2 *conv)
{
	pt_info2x2 *info = &(conv->shared_info);
	// ensure worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	// set job information & state
	info->state = WORKING;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
}
inline void task_start1x2(TwoStageFFTConvolver1x2 *conv)
{
	pt_info1x2 *info = &(conv->shared_info);
	// ensure worker is waiting
	pthread_mutex_lock(&(info->work_mtx));
	// set job information & state
	info->state = WORKING;
	// wake-up signal
	pthread_cond_signal(&(info->work_cond));
	pthread_mutex_unlock(&(info->work_mtx));
}
#endif
void TwoStageFFTConvolver1x1Free(TwoStageFFTConvolver1x1 *conv)
{
#ifdef THREAD
	if (conv->shared_info.state != NOTHING)
	{
		if (conv->shared_info.state == WORKING)
		{
			task_wait1x1(conv);
			thread_exit1x1(conv);
		}
		else
			thread_exit1x1(conv);
	}
#endif
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	if (conv->_headConvolver.bit)
		FFTConvolver1x1Free(&conv->_headConvolver);
	if (conv->_tailConvolver0.bit)
	FFTConvolver1x1Free(&conv->_tailConvolver0);
	if (conv->_tailOutput0)
	{
		free(conv->_tailOutput0);
		conv->_tailOutput0 = 0;
	}
	if (conv->_tailPrecalculated0)
	{
		free(conv->_tailPrecalculated0);
		conv->_tailPrecalculated0 = 0;
	}
	if (conv->_tailOutput)
	{
		free(conv->_tailOutput);
		conv->_tailOutput = 0;
	}
	if (conv->_tailPrecalculated)
	{
		free(conv->_tailPrecalculated);
		conv->_tailPrecalculated = 0;
	}
	if (conv->_tailInput)
	{
		free(conv->_tailInput);
		conv->_tailInput = 0;
	}
	if (conv->_backgroundProcessingInput)
	{
		free(conv->_backgroundProcessingInput);
		conv->_backgroundProcessingInput = 0;
	}
	if (conv->_tailConvolver.bit)
	FFTConvolver1x1Free(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
}
void TwoStageFFTConvolver2x4x2Free(TwoStageFFTConvolver2x4x2 *conv)
{
#ifdef THREAD
	if (conv->shared_info.state != NOTHING)
	{
		if (conv->shared_info.state == WORKING)
		{
			task_wait2x4x2(conv);
			thread_exit2x4x2(conv);
		}
		else
			thread_exit2x4x2(conv);
	}
#endif
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	if (conv->_headConvolver.bit)
		FFTConvolver2x4x2Free(&conv->_headConvolver);
	if (conv->_tailConvolver0.bit)
		FFTConvolver2x4x2Free(&conv->_tailConvolver0);
	if (conv->_tailOutput0[0])
	{
		free(conv->_tailOutput0[0]);
		free(conv->_tailOutput0[1]);
		conv->_tailOutput0[0] = 0;
	}
	if (conv->_tailPrecalculated0[0])
	{
		free(conv->_tailPrecalculated0[0]);
		free(conv->_tailPrecalculated0[1]);
		conv->_tailPrecalculated0[0] = 0;
	}
	if (conv->_tailOutput[0])
	{
		free(conv->_tailOutput[0]);
		free(conv->_tailOutput[1]);
		conv->_tailOutput[0] = 0;
	}
	if (conv->_tailPrecalculated[0])
	{
		free(conv->_tailPrecalculated[0]);
		free(conv->_tailPrecalculated[1]);
		conv->_tailPrecalculated[0] = 0;
	}
	if (conv->_tailInput[0])
	{
		free(conv->_tailInput[0]);
		free(conv->_tailInput[1]);
		conv->_tailInput[0] = 0;
	}
	if (conv->_backgroundProcessingInput[0])
	{
		free(conv->_backgroundProcessingInput[0]);
		free(conv->_backgroundProcessingInput[1]);
		conv->_backgroundProcessingInput[0] = 0;
	}
	if (conv->_tailConvolver.bit)
	FFTConvolver2x4x2Free(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
}
void TwoStageFFTConvolver2x2Free(TwoStageFFTConvolver2x2 *conv)
{
#ifdef THREAD
	if (conv->shared_info.state != NOTHING)
	{
		if (conv->shared_info.state == WORKING)
		{
			task_wait2x2(conv);
			thread_exit2x2(conv);
		}
		else
			thread_exit2x2(conv);
	}
#endif
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	if (conv->_headConvolver.bit)
		FFTConvolver2x2Free(&conv->_headConvolver);
	if (conv->_tailConvolver0.bit)
		FFTConvolver2x2Free(&conv->_tailConvolver0);
	if (conv->_tailOutput0[0])
	{
		free(conv->_tailOutput0[0]);
		free(conv->_tailOutput0[1]);
		conv->_tailOutput0[0] = 0;
	}
	if (conv->_tailPrecalculated0[0])
	{
		free(conv->_tailPrecalculated0[0]);
		free(conv->_tailPrecalculated0[1]);
		conv->_tailPrecalculated0[0] = 0;
	}
	if (conv->_tailOutput[0])
	{
		free(conv->_tailOutput[0]);
		free(conv->_tailOutput[1]);
		conv->_tailOutput[0] = 0;
	}
	if (conv->_tailPrecalculated[0])
	{
		free(conv->_tailPrecalculated[0]);
		free(conv->_tailPrecalculated[1]);
		conv->_tailPrecalculated[0] = 0;
	}
	if (conv->_tailInput[0])
	{
		free(conv->_tailInput[0]);
		free(conv->_tailInput[1]);
		conv->_tailInput[0] = 0;
	}
	if (conv->_backgroundProcessingInput[0])
	{
		free(conv->_backgroundProcessingInput[0]);
		free(conv->_backgroundProcessingInput[1]);
		conv->_backgroundProcessingInput[0] = 0;
	}
	if (conv->_tailConvolver.bit)
		FFTConvolver2x2Free(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
}
void TwoStageFFTConvolver1x2Free(TwoStageFFTConvolver1x2 *conv)
{
#ifdef THREAD
	if (conv->shared_info.state != NOTHING)
	{
		if (conv->shared_info.state == WORKING)
		{
			task_wait1x2(conv);
			thread_exit1x2(conv);
		}
		else
			thread_exit1x2(conv);
	}
#endif
	conv->_headBlockSize = 0;
	conv->_tailBlockSize = 0;
	if (conv->_headConvolver.bit)
		FFTConvolver1x2Free(&conv->_headConvolver);
	if (conv->_tailConvolver0.bit)
		FFTConvolver1x2Free(&conv->_tailConvolver0);
	if (conv->_tailOutput0[0])
	{
		free(conv->_tailOutput0[0]);
		free(conv->_tailOutput0[1]);
		conv->_tailOutput0[0] = 0;
	}
	if (conv->_tailPrecalculated0[0])
	{
		free(conv->_tailPrecalculated0[0]);
		free(conv->_tailPrecalculated0[1]);
		conv->_tailPrecalculated0[0] = 0;
	}
	if (conv->_tailOutput[0])
	{
		free(conv->_tailOutput[0]);
		free(conv->_tailOutput[1]);
		conv->_tailOutput[0] = 0;
	}
	if (conv->_tailPrecalculated[0])
	{
		free(conv->_tailPrecalculated[0]);
		free(conv->_tailPrecalculated[1]);
		conv->_tailPrecalculated[0] = 0;
	}
	if (conv->_tailInput)
	{
		free(conv->_tailInput);
		conv->_tailInput = 0;
	}
	if (conv->_backgroundProcessingInput)
	{
		free(conv->_backgroundProcessingInput);
		conv->_backgroundProcessingInput = 0;
	}
	if (conv->_tailConvolver.bit)
		FFTConvolver1x2Free(&conv->_tailConvolver);
	conv->_tailInputFill = 0;
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
}

#ifndef min
#define min(a,b) (((a)<(b))?(a):(b))
#endif
int TwoStageFFTConvolver1x1LoadImpulseResponse(TwoStageFFTConvolver1x1 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* ir, unsigned int irLen)
{
	if (headBlockSize == 0 || tailBlockSize == 0)
		return 0;
	if (headBlockSize < (unsigned int)1)
		headBlockSize = (unsigned int)1;
	if (headBlockSize > tailBlockSize)
	{
		unsigned int tmp = headBlockSize;
		headBlockSize = tailBlockSize;
		tailBlockSize = tmp;
	}

	// Ignore zeros at the end of the impulse response because they only waste computation time
	while (irLen > 0 && fabsf(ir[irLen - 1]) < FLT_EPSILON)
		--irLen;
	if (irLen == 0)
		return 1;
	if (conv->_headConvolver.bit)
		TwoStageFFTConvolver1x1Free(conv);

	conv->_headBlockSize = upper_power_of_two(headBlockSize);
	conv->_tailBlockSize = upper_power_of_two(tailBlockSize);
	if (conv->_headBlockSize > 524288)
		conv->_headBlockSize = 524288;
	if (conv->_tailBlockSize > 524288)
		conv->_tailBlockSize = 524288;
	const unsigned int headIrLen = min(irLen, conv->_tailBlockSize);
	FFTConvolver1x1LoadImpulseResponse(&conv->_headConvolver, conv->_headBlockSize, ir, headIrLen);
	if (irLen > conv->_tailBlockSize)
	{
		const unsigned int conv1IrLen = min(irLen - conv->_tailBlockSize, conv->_tailBlockSize);
		FFTConvolver1x1LoadImpulseResponse(&conv->_tailConvolver0, conv->_headBlockSize, ir + conv->_tailBlockSize, conv1IrLen);
		conv->_tailOutput0 = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0 = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0, 0, conv->_tailBlockSize * sizeof(float));
	}
	if (irLen > 2 * conv->_tailBlockSize)
	{
		const unsigned int tailIrLen = irLen - (2 * conv->_tailBlockSize);
		FFTConvolver1x1LoadImpulseResponse(&conv->_tailConvolver, conv->_tailBlockSize, ir + (2 * conv->_tailBlockSize), tailIrLen);
		conv->_tailOutput = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput, 0, conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated, 0, conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput = (float*)malloc(conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
		thread_init1x1(conv);
#endif
	}
	if (conv->_tailPrecalculated0 || conv->_tailPrecalculated)
	{
		conv->_tailInput = (float*)malloc(conv->_tailBlockSize * sizeof(float));
	}
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	return 1;
}
int TwoStageFFTConvolver2x4x2LoadImpulseResponse(TwoStageFFTConvolver2x4x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irLL, const float* irLR, const float* irRL, const float* irRR, unsigned int irLen)
{
	if (headBlockSize == 0 || tailBlockSize == 0)
		return 0;
	if (headBlockSize < (unsigned int)1)
		headBlockSize = (unsigned int)1;
	if (headBlockSize > tailBlockSize)
	{
		unsigned int tmp = headBlockSize;
		headBlockSize = tailBlockSize;
		tailBlockSize = tmp;
	}
	// Ignore zeros at the end of the impulse response because they only waste computation time
	while (irLen > 0 && fabsf(irLL[irLen - 1] + irLR[irLen - 1] + irRL[irLen - 1] + irRR[irLen - 1]) < (FLT_EPSILON * 4.0f))
		--irLen;
	if (irLen == 0)
		return 1;

	if (conv->_headConvolver.bit)
		TwoStageFFTConvolver2x4x2Free(conv);

	conv->_headBlockSize = upper_power_of_two(headBlockSize);
	conv->_tailBlockSize = upper_power_of_two(tailBlockSize);
	if (conv->_headBlockSize > 524288)
		conv->_headBlockSize = 524288;
	if (conv->_tailBlockSize > 524288)
		conv->_tailBlockSize = 524288;
	const unsigned int headIrLen = min(irLen, conv->_tailBlockSize);
	FFTConvolver2x4x2LoadImpulseResponse(&conv->_headConvolver, conv->_headBlockSize, irLL, irLR, irRL, irRR, headIrLen);
	if (irLen > conv->_tailBlockSize)
	{
		const unsigned int conv1IrLen = min(irLen - conv->_tailBlockSize, conv->_tailBlockSize);
		FFTConvolver2x4x2LoadImpulseResponse(&conv->_tailConvolver0, conv->_headBlockSize, irLL + conv->_tailBlockSize, irLR + conv->_tailBlockSize, irRL + conv->_tailBlockSize, irRR + conv->_tailBlockSize, conv1IrLen);
		conv->_tailOutput0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[1], 0, conv->_tailBlockSize * sizeof(float));
	}
	if (irLen > 2 * conv->_tailBlockSize)
	{
		const unsigned int tailIrLen = irLen - (2 * conv->_tailBlockSize);
		FFTConvolver2x4x2LoadImpulseResponse(&conv->_tailConvolver, conv->_tailBlockSize, irLL + (2 * conv->_tailBlockSize), irLR + (2 * conv->_tailBlockSize), irRL + (2 * conv->_tailBlockSize), irRR + (2 * conv->_tailBlockSize), tailIrLen);
		conv->_tailOutput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
		thread_init2x4x2(conv);
#endif
	}
	if (conv->_tailPrecalculated0[0] || conv->_tailPrecalculated[0])
	{
		conv->_tailInput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailInput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
	}
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	return 1;
}
int TwoStageFFTConvolver2x2LoadImpulseResponse(TwoStageFFTConvolver2x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irL, const float* irR, unsigned int irLen)
{
	if (headBlockSize == 0 || tailBlockSize == 0)
		return 0;
	if (headBlockSize < (unsigned int)1)
		headBlockSize = (unsigned int)1;
	if (headBlockSize > tailBlockSize)
	{
		unsigned int tmp = headBlockSize;
		headBlockSize = tailBlockSize;
		tailBlockSize = tmp;
	}
	// Ignore zeros at the end of the impulse response because they only waste computation time
	while (irLen > 0 && fabsf(irL[irLen - 1] + irR[irLen - 1]) < (FLT_EPSILON * 2.0f))
		--irLen;
	if (irLen == 0)
		return 1;

	if (conv->_headConvolver.bit)
		TwoStageFFTConvolver2x2Free(conv);

	conv->_headBlockSize = upper_power_of_two(headBlockSize);
	conv->_tailBlockSize = upper_power_of_two(tailBlockSize);
	if (conv->_headBlockSize > 524288)
		conv->_headBlockSize = 524288;
	if (conv->_tailBlockSize > 524288)
		conv->_tailBlockSize = 524288;
	const unsigned int headIrLen = min(irLen, conv->_tailBlockSize);
	FFTConvolver2x2LoadImpulseResponse(&conv->_headConvolver, conv->_headBlockSize, irL, irR, headIrLen);
	if (irLen > conv->_tailBlockSize)
	{
		const unsigned int conv1IrLen = min(irLen - conv->_tailBlockSize, conv->_tailBlockSize);
		FFTConvolver2x2LoadImpulseResponse(&conv->_tailConvolver0, conv->_headBlockSize, irL + conv->_tailBlockSize, irR + conv->_tailBlockSize, conv1IrLen);
		conv->_tailOutput0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[1], 0, conv->_tailBlockSize * sizeof(float));
	}
	if (irLen > 2 * conv->_tailBlockSize)
	{
		const unsigned int tailIrLen = irLen - (2 * conv->_tailBlockSize);
		FFTConvolver2x2LoadImpulseResponse(&conv->_tailConvolver, conv->_tailBlockSize, irL + (2 * conv->_tailBlockSize), irR + (2 * conv->_tailBlockSize), tailIrLen);
		conv->_tailOutput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
		thread_init2x2(conv);
#endif
	}
	if (conv->_tailPrecalculated0[0] || conv->_tailPrecalculated[0])
	{
		conv->_tailInput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailInput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
	}
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	return 1;
}
int TwoStageFFTConvolver1x2LoadImpulseResponse(TwoStageFFTConvolver1x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irL, const float* irR, unsigned int irLen)
{
	if (headBlockSize == 0 || tailBlockSize == 0)
		return 0;
	if (headBlockSize < (unsigned int)1)
		headBlockSize = (unsigned int)1;
	if (headBlockSize > tailBlockSize)
	{
		unsigned int tmp = headBlockSize;
		headBlockSize = tailBlockSize;
		tailBlockSize = tmp;
	}
	// Ignore zeros at the end of the impulse response because they only waste computation time
	while (irLen > 0 && fabsf(irL[irLen - 1] + irR[irLen - 1]) < (FLT_EPSILON * 2.0f))
		--irLen;
	if (irLen == 0)
		return 1;

	if (conv->_headConvolver.bit)
		TwoStageFFTConvolver1x2Free(conv);

	conv->_headBlockSize = upper_power_of_two(headBlockSize);
	conv->_tailBlockSize = upper_power_of_two(tailBlockSize);
	if (conv->_headBlockSize > 524288)
		conv->_headBlockSize = 524288;
	if (conv->_tailBlockSize > 524288)
		conv->_tailBlockSize = 524288;
	const unsigned int headIrLen = min(irLen, conv->_tailBlockSize);
	FFTConvolver1x2LoadImpulseResponse(&conv->_headConvolver, conv->_headBlockSize, irL, irR, headIrLen);
	if (irLen > conv->_tailBlockSize)
	{
		const unsigned int conv1IrLen = min(irLen - conv->_tailBlockSize, conv->_tailBlockSize);
		FFTConvolver1x2LoadImpulseResponse(&conv->_tailConvolver0, conv->_headBlockSize, irL + conv->_tailBlockSize, irR + conv->_tailBlockSize, conv1IrLen);
		conv->_tailOutput0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated0[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated0[1], 0, conv->_tailBlockSize * sizeof(float));
	}
	if (irLen > 2 * conv->_tailBlockSize)
	{
		const unsigned int tailIrLen = irLen - (2 * conv->_tailBlockSize);
		FFTConvolver1x2LoadImpulseResponse(&conv->_tailConvolver, conv->_tailBlockSize, irL + (2 * conv->_tailBlockSize), irR + (2 * conv->_tailBlockSize), tailIrLen);
		conv->_tailOutput[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailOutput[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailOutput[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[0] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		conv->_tailPrecalculated[1] = (float*)malloc(conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[0], 0, conv->_tailBlockSize * sizeof(float));
		memset(conv->_tailPrecalculated[1], 0, conv->_tailBlockSize * sizeof(float));
		conv->_backgroundProcessingInput = (float*)malloc(conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
		thread_init1x2(conv);
#endif
	}
	if (conv->_tailPrecalculated0[0] || conv->_tailPrecalculated[0])
		conv->_tailInput = (float*)malloc(conv->_tailBlockSize * sizeof(float));
	conv->_tailInputFill = 0;
	conv->_precalculatedPos = 0;
	return 1;
}


void TwoStageFFTConvolver1x1Process(TwoStageFFTConvolver1x1 *conv, const float* input, float* output, unsigned int len)
{
	if (conv->_tailInput)
	{
		unsigned int i, j, symIdx, processed = 0;
		while (processed < len)
		{
			const unsigned int remaining = len - processed;
			const unsigned int processing = min(remaining, conv->_headBlockSize - (conv->_tailInputFill % conv->_headBlockSize));
			const int inputBufferWasEmpty = conv->_headConvolver._inputBufferFill == 0;
			const unsigned int inputBufferPos = conv->_headConvolver._inputBufferFill;
			memcpy(conv->_headConvolver._inputBuffer + inputBufferPos, input + processed, processing * sizeof(float));
			// Fill input buffer for tail convolution
			memcpy(conv->_tailInput + conv->_tailInputFill, input + processed, processing * sizeof(float));
			conv->_tailInputFill += processing;

			// Forward FFT
			for (j = 0; j < conv->_headConvolver._blockSize; j++)
				conv->_headConvolver._fftBuffer[conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[j];
			for (j = conv->_headConvolver._blockSize; j < conv->_headConvolver._segSize; j++)
				conv->_headConvolver._fftBuffer[conv->_headConvolver.bit[j]] = 0.0f;
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer, conv->_headConvolver.sine);
			conv->_headConvolver._segmentsRe[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; j++)
			{
				symIdx = conv->_headConvolver._segSize - j;
				conv->_headConvolver._segmentsRe[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[j] + conv->_headConvolver._fftBuffer[symIdx];
				conv->_headConvolver._segmentsIm[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[j] - conv->_headConvolver._fftBuffer[symIdx];
			}

			// Complex multiplication
			const float *reA;
			const float *imA;
			const float *reB;
			const float *imB;
			unsigned int end4 = conv->_headConvolver._fftComplexSize - 1;
			if (inputBufferWasEmpty)
			{
				unsigned int segFrameIndex = (conv->_headConvolver._current + 1) % conv->_headConvolver._segCount;
				if (conv->_headConvolver._segCount > 1)
				{
					float *re = conv->_headConvolver._preMultiplied[0];
					float *im = conv->_headConvolver._preMultiplied[1];
					reA = conv->_headConvolver._segmentsIRRe[1];
					imA = conv->_headConvolver._segmentsIRIm[1];
					reB = conv->_headConvolver._segmentsRe[segFrameIndex];
					imB = conv->_headConvolver._segmentsIm[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						re[j + 0] = reA[j + 0] * reB[j + 0] - imA[j + 0] * imB[j + 0];
						re[j + 1] = reA[j + 1] * reB[j + 1] - imA[j + 1] * imB[j + 1];
						re[j + 2] = reA[j + 2] * reB[j + 2] - imA[j + 2] * imB[j + 2];
						re[j + 3] = reA[j + 3] * reB[j + 3] - imA[j + 3] * imB[j + 3];
						im[j + 0] = reA[j + 0] * imB[j + 0] + imA[j + 0] * reB[j + 0];
						im[j + 1] = reA[j + 1] * imB[j + 1] + imA[j + 1] * reB[j + 1];
						im[j + 2] = reA[j + 2] * imB[j + 2] + imA[j + 2] * reB[j + 2];
						im[j + 3] = reA[j + 3] * imB[j + 3] + imA[j + 3] * reB[j + 3];
					}
					re[end4] = reA[end4] * reB[end4] - imA[end4] * imB[end4];
					im[end4] = reA[end4] * imB[end4] + imA[end4] * reB[end4];
					for (i = 2; i < conv->_headConvolver._segCount; ++i)
					{
						segFrameIndex = (conv->_headConvolver._current + i) % conv->_headConvolver._segCount;
						re = conv->_headConvolver._preMultiplied[0];
						im = conv->_headConvolver._preMultiplied[1];
						reA = conv->_headConvolver._segmentsIRRe[i];
						imA = conv->_headConvolver._segmentsIRIm[i];
						reB = conv->_headConvolver._segmentsRe[segFrameIndex];
						imB = conv->_headConvolver._segmentsIm[segFrameIndex];
						for (j = 0; j < end4; j += 4)
						{
							re[j + 0] += reA[j + 0] * reB[j + 0] - imA[j + 0] * imB[j + 0];
							re[j + 1] += reA[j + 1] * reB[j + 1] - imA[j + 1] * imB[j + 1];
							re[j + 2] += reA[j + 2] * reB[j + 2] - imA[j + 2] * imB[j + 2];
							re[j + 3] += reA[j + 3] * reB[j + 3] - imA[j + 3] * imB[j + 3];
							im[j + 0] += reA[j + 0] * imB[j + 0] + imA[j + 0] * reB[j + 0];
							im[j + 1] += reA[j + 1] * imB[j + 1] + imA[j + 1] * reB[j + 1];
							im[j + 2] += reA[j + 2] * imB[j + 2] + imA[j + 2] * reB[j + 2];
							im[j + 3] += reA[j + 3] * imB[j + 3] + imA[j + 3] * reB[j + 3];
						}
						re[end4] += reA[end4] * reB[end4] - imA[end4] * imB[end4];
						im[end4] += reA[end4] * imB[end4] + imA[end4] * reB[end4];
					}
				}
			}
			reA = conv->_headConvolver._segmentsIRRe[0];
			imA = conv->_headConvolver._segmentsIRIm[0];
			reB = conv->_headConvolver._segmentsRe[conv->_headConvolver._current];
			imB = conv->_headConvolver._segmentsIm[conv->_headConvolver._current];
			const float *srcRe = conv->_headConvolver._preMultiplied[0];
			const float *srcIm = conv->_headConvolver._preMultiplied[1];
			float real, imag;
			conv->_headConvolver._fftBuffer[0] = reB[0] * reA[0] + srcRe[0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; ++j)
			{
				symIdx = conv->_headConvolver._segSize - j;
				real = reB[j] * reA[j] - imB[j] * imA[j] + srcRe[j];
				imag = reB[j] * imA[j] + imB[j] * reA[j] + srcIm[j];
				conv->_headConvolver._fftBuffer[conv->_headConvolver.bit[j]] = (real + imag) * 0.5f;
				conv->_headConvolver._fftBuffer[conv->_headConvolver.bit[symIdx]] = (real - imag) * 0.5f;
			}
			// Backward FFT
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer, conv->_headConvolver.sine);

			// Add overlap
			float *result = output + processed;
			const float *a = conv->_headConvolver._fftBuffer + inputBufferPos;
			const float *b = conv->_headConvolver._overlap + inputBufferPos;
			end4 = (processing >> 2) << 2;
			for (j = 0; j < end4; j += 4)
			{
				result[j + 0] = (a[j + 0] + b[j + 0]) * conv->_headConvolver.gain;
				result[j + 1] = (a[j + 1] + b[j + 1]) * conv->_headConvolver.gain;
				result[j + 2] = (a[j + 2] + b[j + 2]) * conv->_headConvolver.gain;
				result[j + 3] = (a[j + 3] + b[j + 3]) * conv->_headConvolver.gain;
			}
			for (j = end4; j < processing; ++j)
				result[j] = (a[j] + b[j]) * conv->_headConvolver.gain;

			// Input buffer full => Next block
			conv->_headConvolver._inputBufferFill += processing;
			if (conv->_headConvolver._inputBufferFill == conv->_headConvolver._blockSize)
			{
				// Input buffer is empty again now
				memset(conv->_headConvolver._inputBuffer, 0, conv->_headConvolver._blockSize * sizeof(float));
				conv->_headConvolver._inputBufferFill = 0;
				// Save the overlap
				memcpy(conv->_headConvolver._overlap, conv->_headConvolver._fftBuffer + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				// Update current segment
				conv->_headConvolver._current = (conv->_headConvolver._current > 0) ? (conv->_headConvolver._current - 1) : conv->_headConvolver._segCountMinus1;
			}
			// Sum head and tail
			const unsigned int sumBegin = processed;
			const unsigned int sumEnd = processed + processing;
			// Sum: 1st tail block
			if (conv->_tailPrecalculated0)
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					output[i] += conv->_tailPrecalculated0[precalculatedPos];
					++precalculatedPos;
				}
			}
			// Sum: 2nd-Nth tail block
			if (conv->_tailPrecalculated)
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					output[i] += conv->_tailPrecalculated[precalculatedPos];
					++precalculatedPos;
				}
			}

			conv->_precalculatedPos += processing;

			// Convolution: 1st tail block
			if (conv->_tailPrecalculated0 && conv->_tailInputFill % conv->_headBlockSize == 0)
			{
				const unsigned int blockOffset = conv->_tailInputFill - conv->_headBlockSize;
				FFTConvolver1x1Process(&conv->_tailConvolver0, conv->_tailInput + blockOffset, conv->_tailOutput0 + blockOffset, conv->_headBlockSize);
				if (conv->_tailInputFill == conv->_tailBlockSize)
				{
					float *tmp = conv->_tailOutput0;
					conv->_tailOutput0 = conv->_tailPrecalculated0;
					conv->_tailPrecalculated0 = tmp;
				}
			}

			// Convolution: 2nd-Nth tail block (might be done in some background thread)
			if (conv->_tailPrecalculated && conv->_tailInputFill == conv->_tailBlockSize && conv->_backgroundProcessingInput && conv->_tailOutput)
			{
#ifdef THREAD
				if (conv->shared_info.state == WORKING)
					task_wait1x1(conv);
				//        SampleBuffer::Swap(_tailPrecalculated, _tailOutput);
				float *tmp = conv->_tailOutput;
				conv->shared_info._tailOutput = conv->_tailOutput = conv->_tailPrecalculated;
				conv->_tailPrecalculated = tmp;
#else
				float *tmp = conv->_tailOutput;
				conv->_tailOutput = conv->_tailPrecalculated;
				conv->_tailPrecalculated = tmp;
#endif
				memcpy(conv->_backgroundProcessingInput, conv->_tailInput, conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
				task_start1x1(conv);
#else
				FFTConvolver1x1Process(&conv->_tailConvolver, conv->_backgroundProcessingInput, conv->_tailOutput, conv->_tailBlockSize);
#endif
			}
			if (conv->_tailInputFill == conv->_tailBlockSize)
			{
				conv->_tailInputFill = 0;
				conv->_precalculatedPos = 0;
			}
			processed += processing;
		}
	}
	else
	{
		FFTConvolver1x1Process(&conv->_headConvolver, input, output, len);
	}
}
void TwoStageFFTConvolver2x4x2Process(TwoStageFFTConvolver2x4x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len)
{
	if (conv->_tailInput[0])
	{
		unsigned int i, j, symIdx;
		unsigned int processed = 0, processed2 = 0;
		while (processed < len)
		{
			const int inputBufferWasEmpty = conv->_headConvolver._inputBufferFill == 0;
			const unsigned int processing = min(len - processed, conv->_headConvolver._blockSize - conv->_headConvolver._inputBufferFill);
			const unsigned int inputBufferPos = conv->_headConvolver._inputBufferFill;
			memcpy(conv->_headConvolver._inputBuffer[0] + inputBufferPos, x1 + processed, processing * sizeof(float));
			memcpy(conv->_headConvolver._inputBuffer[1] + inputBufferPos, x2 + processed, processing * sizeof(float));
			// Fill input buffer for tail convolution
			memcpy(conv->_tailInput[0] + conv->_tailInputFill, x1 + processed2, processing * sizeof(float));
			memcpy(conv->_tailInput[1] + conv->_tailInputFill, x2 + processed2, processing * sizeof(float));
			conv->_tailInputFill += processing;

			// Forward FFT
			for (j = 0; j < conv->_headConvolver._blockSize; j++)
			{
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[0][j];
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[1][j];
			}
			for (j = conv->_headConvolver._blockSize; j < conv->_headConvolver._segSize; j++)
			{
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = 0.0f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = 0.0f;
			}
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[0][0];
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[1], conv->_headConvolver.sine);
			conv->_headConvolver._segmentsReRight[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[1][0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; j++)
			{
				symIdx = conv->_headConvolver._segSize - j;
				conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] + conv->_headConvolver._fftBuffer[0][symIdx];
				conv->_headConvolver._segmentsImLeft[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] - conv->_headConvolver._fftBuffer[0][symIdx];
				conv->_headConvolver._segmentsReRight[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[1][j] + conv->_headConvolver._fftBuffer[1][symIdx];
				conv->_headConvolver._segmentsImRight[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[1][j] - conv->_headConvolver._fftBuffer[1][symIdx];
			}

			// Complex multiplication
			const float *reALL, *imALL, *reALR, *imALR, *reARL, *imARL, *reARR, *imARR;
			const float *reBLeft;
			const float *imBLeft;
			const float *reBRight;
			const float *imBRight;
			unsigned int end4 = conv->_headConvolver._fftComplexSize - 1;
			if (inputBufferWasEmpty)
			{
				unsigned int segFrameIndex = (conv->_headConvolver._current + 1) % conv->_headConvolver._segCount;
				if (conv->_headConvolver._segCount > 1)
				{
					float *reLeft = conv->_headConvolver._preMultiplied[0][0];
					float *imLeft = conv->_headConvolver._preMultiplied[0][1];
					float *reRight = conv->_headConvolver._preMultiplied[1][0];
					float *imRight = conv->_headConvolver._preMultiplied[1][1];
					reALL = conv->_headConvolver._segmentsLLIRRe[1];
					imALL = conv->_headConvolver._segmentsLLIRIm[1];
					reALR = conv->_headConvolver._segmentsLRIRRe[1];
					imALR = conv->_headConvolver._segmentsLRIRIm[1];
					reARL = conv->_headConvolver._segmentsRLIRRe[1];
					imARL = conv->_headConvolver._segmentsRLIRIm[1];
					reARR = conv->_headConvolver._segmentsRRIRRe[1];
					imARR = conv->_headConvolver._segmentsRRIRIm[1];
					reBLeft = conv->_headConvolver._segmentsReLeft[segFrameIndex];
					imBLeft = conv->_headConvolver._segmentsImLeft[segFrameIndex];
					reBRight = conv->_headConvolver._segmentsReRight[segFrameIndex];
					imBRight = conv->_headConvolver._segmentsImRight[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] = (reARL[j + 0] * reBRight[j + 0] - imARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0]);
						reLeft[j + 1] = (reARL[j + 1] * reBRight[j + 1] - imARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1]);
						reLeft[j + 2] = (reARL[j + 2] * reBRight[j + 2] - imARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2]);
						reLeft[j + 3] = (reARL[j + 3] * reBRight[j + 3] - imARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3]);
						imLeft[j + 0] = (imARL[j + 0] * reBRight[j + 0] + reARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0]);
						imLeft[j + 1] = (imARL[j + 1] * reBRight[j + 1] + reARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1]);
						imLeft[j + 2] = (imARL[j + 2] * reBRight[j + 2] + reARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2]);
						imLeft[j + 3] = (imARL[j + 3] * reBRight[j + 3] + reARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3]);
						reRight[j + 0] = (reALR[j + 0] * reBLeft[j + 0] - imALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0]);
						reRight[j + 1] = (reALR[j + 1] * reBLeft[j + 1] - imALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1]);
						reRight[j + 2] = (reALR[j + 2] * reBLeft[j + 2] - imALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2]);
						reRight[j + 3] = (reALR[j + 3] * reBLeft[j + 3] - imALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3]);
						imRight[j + 0] = (imALR[j + 0] * reBLeft[j + 0] + reALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0]);
						imRight[j + 1] = (imALR[j + 1] * reBLeft[j + 1] + reALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1]);
						imRight[j + 2] = (imALR[j + 2] * reBLeft[j + 2] + reALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2]);
						imRight[j + 3] = (imALR[j + 3] * reBLeft[j + 3] + reALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3]);
					}
					reLeft[end4] = (reARL[end4] * reBRight[end4] - imARL[end4] * imBRight[end4]) + (reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4]);
					imLeft[end4] = (imARL[end4] * reBRight[end4] + reARL[end4] * imBRight[end4]) + (reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4]);
					reRight[end4] = (reALR[end4] * reBLeft[end4] - imALR[end4] * imBLeft[end4]) + (reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4]);
					imRight[end4] = (imALR[end4] * reBLeft[end4] + reALR[end4] * imBLeft[end4]) + (reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4]);
					for (unsigned int i = 2; i < conv->_headConvolver._segCount; ++i)
					{
						segFrameIndex = (conv->_headConvolver._current + i) % conv->_headConvolver._segCount;
						reLeft = conv->_headConvolver._preMultiplied[0][0];
						imLeft = conv->_headConvolver._preMultiplied[0][1];
						reRight = conv->_headConvolver._preMultiplied[1][0];
						imRight = conv->_headConvolver._preMultiplied[1][1];
						reALL = conv->_headConvolver._segmentsLLIRRe[i];
						imALL = conv->_headConvolver._segmentsLLIRIm[i];
						reALR = conv->_headConvolver._segmentsLRIRRe[i];
						imALR = conv->_headConvolver._segmentsLRIRIm[i];
						reARL = conv->_headConvolver._segmentsRLIRRe[i];
						imARL = conv->_headConvolver._segmentsRLIRIm[i];
						reARR = conv->_headConvolver._segmentsRRIRRe[i];
						imARR = conv->_headConvolver._segmentsRRIRIm[i];
						reBLeft = conv->_headConvolver._segmentsReLeft[segFrameIndex];
						imBLeft = conv->_headConvolver._segmentsImLeft[segFrameIndex];
						reBRight = conv->_headConvolver._segmentsReRight[segFrameIndex];
						imBRight = conv->_headConvolver._segmentsImRight[segFrameIndex];
						for (j = 0; j < end4; j += 4)
						{
							reLeft[j + 0] += (reARL[j + 0] * reBRight[j + 0] - imARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0]);
							reLeft[j + 1] += (reARL[j + 1] * reBRight[j + 1] - imARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1]);
							reLeft[j + 2] += (reARL[j + 2] * reBRight[j + 2] - imARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2]);
							reLeft[j + 3] += (reARL[j + 3] * reBRight[j + 3] - imARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3]);
							imLeft[j + 0] += (imARL[j + 0] * reBRight[j + 0] + reARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0]);
							imLeft[j + 1] += (imARL[j + 1] * reBRight[j + 1] + reARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1]);
							imLeft[j + 2] += (imARL[j + 2] * reBRight[j + 2] + reARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2]);
							imLeft[j + 3] += (imARL[j + 3] * reBRight[j + 3] + reARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3]);
							reRight[j + 0] += (reALR[j + 0] * reBLeft[j + 0] - imALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0]);
							reRight[j + 1] += (reALR[j + 1] * reBLeft[j + 1] - imALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1]);
							reRight[j + 2] += (reALR[j + 2] * reBLeft[j + 2] - imALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2]);
							reRight[j + 3] += (reALR[j + 3] * reBLeft[j + 3] - imALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3]);
							imRight[j + 0] += (imALR[j + 0] * reBLeft[j + 0] + reALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0]);
							imRight[j + 1] += (imALR[j + 1] * reBLeft[j + 1] + reALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1]);
							imRight[j + 2] += (imALR[j + 2] * reBLeft[j + 2] + reALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2]);
							imRight[j + 3] += (imALR[j + 3] * reBLeft[j + 3] + reALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3]);
						}
						reLeft[end4] += (reARL[end4] * reBRight[end4] - imARL[end4] * imBRight[end4]) + (reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4]);
						imLeft[end4] += (imARL[end4] * reBRight[end4] + reARL[end4] * imBRight[end4]) + (reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4]);
						reRight[end4] += (reALR[end4] * reBLeft[end4] - imALR[end4] * imBLeft[end4]) + (reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4]);
						imRight[end4] += (imALR[end4] * reBLeft[end4] + reALR[end4] * imBLeft[end4]) + (reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4]);
					}
				}
			}
			reALL = conv->_headConvolver._segmentsLLIRRe[0];
			imALL = conv->_headConvolver._segmentsLLIRIm[0];
			reALR = conv->_headConvolver._segmentsLRIRRe[0];
			imALR = conv->_headConvolver._segmentsLRIRIm[0];
			reARL = conv->_headConvolver._segmentsRLIRRe[0];
			imARL = conv->_headConvolver._segmentsRLIRIm[0];
			reARR = conv->_headConvolver._segmentsRRIRRe[0];
			imARR = conv->_headConvolver._segmentsRRIRIm[0];
			reBLeft = conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current];
			imBLeft = conv->_headConvolver._segmentsImLeft[conv->_headConvolver._current];
			reBRight = conv->_headConvolver._segmentsReRight[conv->_headConvolver._current];
			imBRight = conv->_headConvolver._segmentsImRight[conv->_headConvolver._current];
			const float *src1Re = conv->_headConvolver._preMultiplied[0][0];
			const float *src1Im = conv->_headConvolver._preMultiplied[0][1];
			const float *src2Re = conv->_headConvolver._preMultiplied[1][0];
			const float *src2Im = conv->_headConvolver._preMultiplied[1][1];
			float realL, imagL, realR, imagR;
			conv->_headConvolver._fftBuffer[0][0] = (reARL[0] * reBRight[0]) + (reALL[0] * reBLeft[0]) + src1Re[0];
			conv->_headConvolver._fftBuffer[1][0] = (reALR[0] * reBLeft[0]) + (reARR[0] * reBRight[0]) + src2Re[0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; ++j)
			{
				symIdx = conv->_headConvolver._segSize - j;
				realL = (reARL[j] * reBRight[j] - imARL[j] * imBRight[j]) + (reALL[j] * reBLeft[j] - imALL[j] * imBLeft[j]) + src1Re[j];
				imagL = (imARL[j] * reBRight[j] + reARL[j] * imBRight[j]) + (reALL[j] * imBLeft[j] + imALL[j] * reBLeft[j]) + src1Im[j];
				realR = (reALR[j] * reBLeft[j] - imALR[j] * imBLeft[j]) + (reARR[j] * reBRight[j] - imARR[j] * imBRight[j]) + src2Re[j];
				imagR = (imALR[j] * reBLeft[j] + reALR[j] * imBLeft[j]) + (reARR[j] * imBRight[j] + imARR[j] * reBRight[j]) + src2Im[j];
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = (realL + imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[symIdx]] = (realL - imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = (realR + imagR) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[symIdx]] = (realR - imagR) * 0.5f;
			}
			// Backward FFT
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[1], conv->_headConvolver.sine);

			// Add overlap
			float *result1 = y1 + processed;
			const float *a1 = conv->_headConvolver._fftBuffer[0] + inputBufferPos;
			const float *b1 = conv->_headConvolver._overlap[0] + inputBufferPos;
			float *result2 = y2 + processed;
			const float *a2 = conv->_headConvolver._fftBuffer[1] + inputBufferPos;
			const float *b2 = conv->_headConvolver._overlap[1] + inputBufferPos;
			end4 = (processing >> 2) << 2;
			for (j = 0; j < end4; j += 4)
			{
				result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->_headConvolver.gain;
				result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->_headConvolver.gain;
				result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->_headConvolver.gain;
				result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->_headConvolver.gain;
				result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->_headConvolver.gain;
				result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->_headConvolver.gain;
				result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->_headConvolver.gain;
				result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->_headConvolver.gain;
			}
			for (j = end4; j < processing; ++j)
			{
				result1[j] = (a1[j] + b1[j]) * conv->_headConvolver.gain;
				result2[j] = (a2[j] + b2[j]) * conv->_headConvolver.gain;
			}

			// Sum head and tail
			const unsigned int sumBegin = processed2;
			const unsigned int sumEnd = processed2 + processing;
			// Sum: 1st tail block
			if (conv->_tailPrecalculated0[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated0[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated0[1][precalculatedPos];
					++precalculatedPos;
				}
			}
			// Sum: 2nd-Nth tail block
			if (conv->_tailPrecalculated[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated[1][precalculatedPos];
					++precalculatedPos;
				}
			}

			conv->_precalculatedPos += processing;

			// Convolution: 1st tail block
			if (conv->_tailPrecalculated0[0] && conv->_tailInputFill % conv->_headBlockSize == 0)
			{
				const unsigned int blockOffset = conv->_tailInputFill - conv->_headBlockSize;
				FFTConvolver2x4x2Process(&conv->_tailConvolver0, conv->_tailInput[0] + blockOffset, conv->_tailInput[1] + blockOffset, conv->_tailOutput0[0] + blockOffset, conv->_tailOutput0[1] + blockOffset, conv->_headBlockSize);
				if (conv->_tailInputFill == conv->_tailBlockSize)
				{
					float *tmp = conv->_tailOutput0[0];
					conv->_tailOutput0[0] = conv->_tailPrecalculated0[0];
					conv->_tailPrecalculated0[0] = tmp;
					tmp = conv->_tailOutput0[1];
					conv->_tailOutput0[1] = conv->_tailPrecalculated0[1];
					conv->_tailPrecalculated0[1] = tmp;
				}
			}

			// Convolution: 2nd-Nth tail block (might be done in some background thread)
			if (conv->_tailPrecalculated[0] && conv->_tailInputFill == conv->_tailBlockSize && conv->_backgroundProcessingInput[0])
			{
#ifdef THREAD
				if (conv->shared_info.state == WORKING)
					task_wait2x4x2(conv);
				//        SampleBuffer::Swap(_tailPrecalculated, _tailOutput);
				float *tmp = conv->_tailOutput[0];
				conv->shared_info._tailOutput[0] = conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->shared_info._tailOutput[1] = conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#else
				float *tmp = conv->_tailOutput[0];
				conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#endif
				memcpy(conv->_backgroundProcessingInput[0], conv->_tailInput[0], conv->_tailBlockSize * sizeof(float));
				memcpy(conv->_backgroundProcessingInput[1], conv->_tailInput[1], conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
				task_start2x4x2(conv);
#else
				FFTConvolver2x4x2Process(&conv->_tailConvolver, conv->_backgroundProcessingInput[0], conv->_backgroundProcessingInput[1], conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
#endif
			}
			if (conv->_tailInputFill == conv->_tailBlockSize)
			{
				conv->_tailInputFill = 0;
				conv->_precalculatedPos = 0;
			}
			processed2 += processing;
			// Input buffer full => Next block
			conv->_headConvolver._inputBufferFill += processing;
			if (conv->_headConvolver._inputBufferFill == conv->_headConvolver._blockSize)
			{
				// Input buffer is empty again now
				memset(conv->_headConvolver._inputBuffer[0], 0, conv->_headConvolver._blockSize * sizeof(float));
				memset(conv->_headConvolver._inputBuffer[1], 0, conv->_headConvolver._blockSize * sizeof(float));
				conv->_headConvolver._inputBufferFill = 0;
				// Save the overlap
				memcpy(conv->_headConvolver._overlap[0], conv->_headConvolver._fftBuffer[0] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				memcpy(conv->_headConvolver._overlap[1], conv->_headConvolver._fftBuffer[1] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				// Update current segment
				conv->_headConvolver._current = (conv->_headConvolver._current > 0) ? (conv->_headConvolver._current - 1) : conv->_headConvolver._segCountMinus1;
			}
			processed += processing;
		}
	}
	else
	{
		FFTConvolver2x4x2Process(&conv->_headConvolver, x1, x2, y1, y2, len);
	}
}
void TwoStageFFTConvolver2x2Process(TwoStageFFTConvolver2x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len)
{
	if (conv->_tailInput[0])
	{
		unsigned int i, j, symIdx, processed = 0;
		while (processed < len)
		{
			const unsigned int remaining = len - processed;
			const unsigned int processing = min(remaining, conv->_headBlockSize - (conv->_tailInputFill % conv->_headBlockSize));
			const int inputBufferWasEmpty = conv->_headConvolver._inputBufferFill == 0;
			const unsigned int inputBufferPos = conv->_headConvolver._inputBufferFill;
			memcpy(conv->_headConvolver._inputBuffer[0] + inputBufferPos, x1 + processed, processing * sizeof(float));
			memcpy(conv->_headConvolver._inputBuffer[1] + inputBufferPos, x2 + processed, processing * sizeof(float));
			// Fill input buffer for tail convolution
			memcpy(conv->_tailInput[0] + conv->_tailInputFill, x1 + processed, processing * sizeof(float));
			memcpy(conv->_tailInput[1] + conv->_tailInputFill, x2 + processed, processing * sizeof(float));
			conv->_tailInputFill += processing;

			// Forward FFT
			for (j = 0; j < conv->_headConvolver._blockSize; j++)
			{
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[0][j];
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[1][j];
			}
			for (j = conv->_headConvolver._blockSize; j < conv->_headConvolver._segSize; j++)
			{
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = 0.0f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = 0.0f;
			}
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[0][0];
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[1], conv->_headConvolver.sine);
			conv->_headConvolver._segmentsReRight[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[1][0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; j++)
			{
				symIdx = conv->_headConvolver._segSize - j;
				conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] + conv->_headConvolver._fftBuffer[0][symIdx];
				conv->_headConvolver._segmentsImLeft[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] - conv->_headConvolver._fftBuffer[0][symIdx];
				conv->_headConvolver._segmentsReRight[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[1][j] + conv->_headConvolver._fftBuffer[1][symIdx];
				conv->_headConvolver._segmentsImRight[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[1][j] - conv->_headConvolver._fftBuffer[1][symIdx];
			}

			// Complex multiplication
			const float *reALL, *imALL, *reARR, *imARR;
			const float *reBLeft;
			const float *imBLeft;
			const float *reBRight;
			const float *imBRight;
			unsigned int end4 = conv->_headConvolver._fftComplexSize - 1;
			if (inputBufferWasEmpty)
			{
				unsigned int segFrameIndex = (conv->_headConvolver._current + 1) % conv->_headConvolver._segCount;
				if (conv->_headConvolver._segCount > 1)
				{
					float *reLeft = conv->_headConvolver._preMultiplied[0][0];
					float *imLeft = conv->_headConvolver._preMultiplied[0][1];
					float *reRight = conv->_headConvolver._preMultiplied[1][0];
					float *imRight = conv->_headConvolver._preMultiplied[1][1];
					reALL = conv->_headConvolver._segmentsLLIRRe[1];
					imALL = conv->_headConvolver._segmentsLLIRIm[1];
					reARR = conv->_headConvolver._segmentsRRIRRe[1];
					imARR = conv->_headConvolver._segmentsRRIRIm[1];
					reBLeft = conv->_headConvolver._segmentsReLeft[segFrameIndex];
					imBLeft = conv->_headConvolver._segmentsImLeft[segFrameIndex];
					reBRight = conv->_headConvolver._segmentsReRight[segFrameIndex];
					imBRight = conv->_headConvolver._segmentsImRight[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] = reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0];
						reLeft[j + 1] = reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1];
						reLeft[j + 2] = reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2];
						reLeft[j + 3] = reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3];
						imLeft[j + 0] = reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0];
						imLeft[j + 1] = reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1];
						imLeft[j + 2] = reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2];
						imLeft[j + 3] = reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3];
						reRight[j + 0] = reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0];
						reRight[j + 1] = reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1];
						reRight[j + 2] = reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2];
						reRight[j + 3] = reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3];
						imRight[j + 0] = reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0];
						imRight[j + 1] = reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1];
						imRight[j + 2] = reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2];
						imRight[j + 3] = reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3];
					}
					reLeft[end4] = reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4];
					imLeft[end4] = reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4];
					reRight[end4] = reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4];
					imRight[end4] = reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4];
					for (unsigned int i = 2; i < conv->_headConvolver._segCount; ++i)
					{
						segFrameIndex = (conv->_headConvolver._current + i) % conv->_headConvolver._segCount;
						reLeft = conv->_headConvolver._preMultiplied[0][0];
						imLeft = conv->_headConvolver._preMultiplied[0][1];
						reRight = conv->_headConvolver._preMultiplied[1][0];
						imRight = conv->_headConvolver._preMultiplied[1][1];
						reALL = conv->_headConvolver._segmentsLLIRRe[i];
						imALL = conv->_headConvolver._segmentsLLIRIm[i];
						reARR = conv->_headConvolver._segmentsRRIRRe[i];
						imARR = conv->_headConvolver._segmentsRRIRIm[i];
						reBLeft = conv->_headConvolver._segmentsReLeft[segFrameIndex];
						imBLeft = conv->_headConvolver._segmentsImLeft[segFrameIndex];
						reBRight = conv->_headConvolver._segmentsReRight[segFrameIndex];
						imBRight = conv->_headConvolver._segmentsImRight[segFrameIndex];
						for (j = 0; j < end4; j += 4)
						{
							reLeft[j + 0] += reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0];
							reLeft[j + 1] += reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1];
							reLeft[j + 2] += reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2];
							reLeft[j + 3] += reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3];
							imLeft[j + 0] += reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0];
							imLeft[j + 1] += reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1];
							imLeft[j + 2] += reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2];
							imLeft[j + 3] += reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3];
							reRight[j + 0] += reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0];
							reRight[j + 1] += reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1];
							reRight[j + 2] += reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2];
							reRight[j + 3] += reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3];
							imRight[j + 0] += reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0];
							imRight[j + 1] += reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1];
							imRight[j + 2] += reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2];
							imRight[j + 3] += reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3];
						}
						reLeft[end4] += reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4];
						imLeft[end4] += reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4];
						reRight[end4] += reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4];
						imRight[end4] += reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4];
					}
				}
			}
			reALL = conv->_headConvolver._segmentsLLIRRe[0];
			imALL = conv->_headConvolver._segmentsLLIRIm[0];
			reARR = conv->_headConvolver._segmentsRRIRRe[0];
			imARR = conv->_headConvolver._segmentsRRIRIm[0];
			reBLeft = conv->_headConvolver._segmentsReLeft[conv->_headConvolver._current];
			imBLeft = conv->_headConvolver._segmentsImLeft[conv->_headConvolver._current];
			reBRight = conv->_headConvolver._segmentsReRight[conv->_headConvolver._current];
			imBRight = conv->_headConvolver._segmentsImRight[conv->_headConvolver._current];
			const float *src1Re = conv->_headConvolver._preMultiplied[0][0];
			const float *src1Im = conv->_headConvolver._preMultiplied[0][1];
			const float *src2Re = conv->_headConvolver._preMultiplied[1][0];
			const float *src2Im = conv->_headConvolver._preMultiplied[1][1];
			float realL, imagL, realR, imagR;
			conv->_headConvolver._fftBuffer[0][0] = (reALL[0] * reBLeft[0]) + src1Re[0];
			conv->_headConvolver._fftBuffer[1][0] = (reARR[0] * reBRight[0]) + src2Re[0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; ++j)
			{
				symIdx = conv->_headConvolver._segSize - j;
				realL = (reALL[j] * reBLeft[j] - imALL[j] * imBLeft[j]) + src1Re[j];
				imagL = (reALL[j] * imBLeft[j] + imALL[j] * reBLeft[j]) + src1Im[j];
				realR = (reARR[j] * reBRight[j] - imARR[j] * imBRight[j]) + src2Re[j];
				imagR = (reARR[j] * imBRight[j] + imARR[j] * reBRight[j]) + src2Im[j];
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = (realL + imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[symIdx]] = (realL - imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = (realR + imagR) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[symIdx]] = (realR - imagR) * 0.5f;
			}
			// Backward FFT
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[1], conv->_headConvolver.sine);

			// Add overlap
			float *result1 = y1 + processed;
			const float *a1 = conv->_headConvolver._fftBuffer[0] + inputBufferPos;
			const float *b1 = conv->_headConvolver._overlap[0] + inputBufferPos;
			float *result2 = y2 + processed;
			const float *a2 = conv->_headConvolver._fftBuffer[1] + inputBufferPos;
			const float *b2 = conv->_headConvolver._overlap[1] + inputBufferPos;
			end4 = (processing >> 2) << 2;
			for (j = 0; j < end4; j += 4)
			{
				result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->_headConvolver.gain;
				result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->_headConvolver.gain;
				result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->_headConvolver.gain;
				result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->_headConvolver.gain;
				result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->_headConvolver.gain;
				result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->_headConvolver.gain;
				result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->_headConvolver.gain;
				result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->_headConvolver.gain;
			}
			for (j = end4; j < processing; ++j)
			{
				result1[j] = (a1[j] + b1[j]) * conv->_headConvolver.gain;
				result2[j] = (a2[j] + b2[j]) * conv->_headConvolver.gain;
			}

			// Input buffer full => Next block
			conv->_headConvolver._inputBufferFill += processing;
			if (conv->_headConvolver._inputBufferFill == conv->_headConvolver._blockSize)
			{
				// Input buffer is empty again now
				memset(conv->_headConvolver._inputBuffer[0], 0, conv->_headConvolver._blockSize * sizeof(float));
				memset(conv->_headConvolver._inputBuffer[1], 0, conv->_headConvolver._blockSize * sizeof(float));
				conv->_headConvolver._inputBufferFill = 0;
				// Save the overlap
				memcpy(conv->_headConvolver._overlap[0], conv->_headConvolver._fftBuffer[0] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				memcpy(conv->_headConvolver._overlap[1], conv->_headConvolver._fftBuffer[1] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				// Update current segment
				conv->_headConvolver._current = (conv->_headConvolver._current > 0) ? (conv->_headConvolver._current - 1) : conv->_headConvolver._segCountMinus1;
			}
			// Sum head and tail
			const unsigned int sumBegin = processed;
			const unsigned int sumEnd = processed + processing;
			// Sum: 1st tail block
			if (conv->_tailPrecalculated0[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated0[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated0[1][precalculatedPos];
					++precalculatedPos;
				}
			}
			// Sum: 2nd-Nth tail block
			if (conv->_tailPrecalculated[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated[1][precalculatedPos];
					++precalculatedPos;
				}
			}

			conv->_precalculatedPos += processing;

			// Convolution: 1st tail block
			if (conv->_tailPrecalculated0[0] && conv->_tailInputFill % conv->_headBlockSize == 0)
			{
				const unsigned int blockOffset = conv->_tailInputFill - conv->_headBlockSize;
				FFTConvolver2x2Process(&conv->_tailConvolver0, conv->_tailInput[0] + blockOffset, conv->_tailInput[1] + blockOffset, conv->_tailOutput0[0] + blockOffset, conv->_tailOutput0[1] + blockOffset, conv->_headBlockSize);
				if (conv->_tailInputFill == conv->_tailBlockSize)
				{
					float *tmp = conv->_tailOutput0[0];
					conv->_tailOutput0[0] = conv->_tailPrecalculated0[0];
					conv->_tailPrecalculated0[0] = tmp;
					tmp = conv->_tailOutput0[1];
					conv->_tailOutput0[1] = conv->_tailPrecalculated0[1];
					conv->_tailPrecalculated0[1] = tmp;
				}
			}

			// Convolution: 2nd-Nth tail block (might be done in some background thread)
			if (conv->_tailPrecalculated[0] && conv->_tailInputFill == conv->_tailBlockSize && conv->_backgroundProcessingInput[0])
			{
#ifdef THREAD
				if (conv->shared_info.state == WORKING)
					task_wait2x2(conv);
				//        SampleBuffer::Swap(_tailPrecalculated, _tailOutput);
				float *tmp = conv->_tailOutput[0];
				conv->shared_info._tailOutput[0] = conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->shared_info._tailOutput[1] = conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#else
				float *tmp = conv->_tailOutput[0];
				conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#endif
				memcpy(conv->_backgroundProcessingInput[0], conv->_tailInput[0], conv->_tailBlockSize * sizeof(float));
				memcpy(conv->_backgroundProcessingInput[1], conv->_tailInput[1], conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
				task_start2x2(conv);
#else
				FFTConvolver2x2Process(&conv->_tailConvolver, conv->_backgroundProcessingInput[0], conv->_backgroundProcessingInput[1], conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
#endif
			}
			if (conv->_tailInputFill == conv->_tailBlockSize)
			{
				conv->_tailInputFill = 0;
				conv->_precalculatedPos = 0;
			}
			processed += processing;
		}
	}
	else
	{
		FFTConvolver2x2Process(&conv->_headConvolver, x1, x2, y1, y2, len);
	}
}
void TwoStageFFTConvolver1x2Process(TwoStageFFTConvolver1x2 *conv, const float* x, float* y1, float* y2, unsigned int len)
{
	if (conv->_tailInput)
	{
		unsigned int i, j, symIdx, processed = 0;
		while (processed < len)
		{
			const unsigned int remaining = len - processed;
			const unsigned int processing = min(remaining, conv->_headBlockSize - (conv->_tailInputFill % conv->_headBlockSize));
			const int inputBufferWasEmpty = conv->_headConvolver._inputBufferFill == 0;
			const unsigned int inputBufferPos = conv->_headConvolver._inputBufferFill;
			memcpy(conv->_headConvolver._inputBuffer + inputBufferPos, x + processed, processing * sizeof(float));
			// Fill input buffer for tail convolution
			memcpy(conv->_tailInput + conv->_tailInputFill, x + processed, processing * sizeof(float));
			conv->_tailInputFill += processing;

			// Forward FFT
			for (j = 0; j < conv->_headConvolver._blockSize; j++)
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = conv->_headConvolver._inputBuffer[j];
			for (j = conv->_headConvolver._blockSize; j < conv->_headConvolver._segSize; j++)
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = 0.0f;
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver._segmentsRe[conv->_headConvolver._current][0] = conv->_headConvolver._fftBuffer[0][0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; j++)
			{
				symIdx = conv->_headConvolver._segSize - j;
				conv->_headConvolver._segmentsRe[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] + conv->_headConvolver._fftBuffer[0][symIdx];
				conv->_headConvolver._segmentsIm[conv->_headConvolver._current][j] = conv->_headConvolver._fftBuffer[0][j] - conv->_headConvolver._fftBuffer[0][symIdx];
			}

			// Complex multiplication
			const float *reALL, *imALL, *reARR, *imARR;
			const float *reB;
			const float *imB;
			unsigned int end4 = conv->_headConvolver._fftComplexSize - 1;
			if (inputBufferWasEmpty)
			{
				unsigned int segFrameIndex = (conv->_headConvolver._current + 1) % conv->_headConvolver._segCount;
				if (conv->_headConvolver._segCount > 1)
				{
					float *reLeft = conv->_headConvolver._preMultiplied[0][0];
					float *imLeft = conv->_headConvolver._preMultiplied[0][1];
					float *reRight = conv->_headConvolver._preMultiplied[1][0];
					float *imRight = conv->_headConvolver._preMultiplied[1][1];
					reALL = conv->_headConvolver._segmentsLLIRRe[1];
					imALL = conv->_headConvolver._segmentsLLIRIm[1];
					reARR = conv->_headConvolver._segmentsRRIRRe[1];
					imARR = conv->_headConvolver._segmentsRRIRIm[1];
					reB = conv->_headConvolver._segmentsRe[segFrameIndex];
					imB = conv->_headConvolver._segmentsIm[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] = reALL[j + 0] * reB[j + 0] - imALL[j + 0] * imB[j + 0];
						reLeft[j + 1] = reALL[j + 1] * reB[j + 1] - imALL[j + 1] * imB[j + 1];
						reLeft[j + 2] = reALL[j + 2] * reB[j + 2] - imALL[j + 2] * imB[j + 2];
						reLeft[j + 3] = reALL[j + 3] * reB[j + 3] - imALL[j + 3] * imB[j + 3];
						imLeft[j + 0] = reALL[j + 0] * imB[j + 0] + imALL[j + 0] * reB[j + 0];
						imLeft[j + 1] = reALL[j + 1] * imB[j + 1] + imALL[j + 1] * reB[j + 1];
						imLeft[j + 2] = reALL[j + 2] * imB[j + 2] + imALL[j + 2] * reB[j + 2];
						imLeft[j + 3] = reALL[j + 3] * imB[j + 3] + imALL[j + 3] * reB[j + 3];
						reRight[j + 0] = reARR[j + 0] * reB[j + 0] - imARR[j + 0] * imB[j + 0];
						reRight[j + 1] = reARR[j + 1] * reB[j + 1] - imARR[j + 1] * imB[j + 1];
						reRight[j + 2] = reARR[j + 2] * reB[j + 2] - imARR[j + 2] * imB[j + 2];
						reRight[j + 3] = reARR[j + 3] * reB[j + 3] - imARR[j + 3] * imB[j + 3];
						imRight[j + 0] = reARR[j + 0] * imB[j + 0] + imARR[j + 0] * reB[j + 0];
						imRight[j + 1] = reARR[j + 1] * imB[j + 1] + imARR[j + 1] * reB[j + 1];
						imRight[j + 2] = reARR[j + 2] * imB[j + 2] + imARR[j + 2] * reB[j + 2];
						imRight[j + 3] = reARR[j + 3] * imB[j + 3] + imARR[j + 3] * reB[j + 3];
					}
					reLeft[end4] = reALL[end4] * reB[end4] - imALL[end4] * imB[end4];
					imLeft[end4] = reALL[end4] * imB[end4] + imALL[end4] * reB[end4];
					reRight[end4] = reARR[end4] * reB[end4] - imARR[end4] * imB[end4];
					imRight[end4] = reARR[end4] * imB[end4] + imARR[end4] * reB[end4];
					for (unsigned int i = 2; i < conv->_headConvolver._segCount; ++i)
					{
						segFrameIndex = (conv->_headConvolver._current + i) % conv->_headConvolver._segCount;
						reLeft = conv->_headConvolver._preMultiplied[0][0];
						imLeft = conv->_headConvolver._preMultiplied[0][1];
						reRight = conv->_headConvolver._preMultiplied[1][0];
						imRight = conv->_headConvolver._preMultiplied[1][1];
						reALL = conv->_headConvolver._segmentsLLIRRe[i];
						imALL = conv->_headConvolver._segmentsLLIRIm[i];
						reARR = conv->_headConvolver._segmentsRRIRRe[i];
						imARR = conv->_headConvolver._segmentsRRIRIm[i];
						reB = conv->_headConvolver._segmentsRe[segFrameIndex];
						imB = conv->_headConvolver._segmentsIm[segFrameIndex];
						for (j = 0; j < end4; j += 4)
						{
							reLeft[j + 0] += reALL[j + 0] * reB[j + 0] - imALL[j + 0] * imB[j + 0];
							reLeft[j + 1] += reALL[j + 1] * reB[j + 1] - imALL[j + 1] * imB[j + 1];
							reLeft[j + 2] += reALL[j + 2] * reB[j + 2] - imALL[j + 2] * imB[j + 2];
							reLeft[j + 3] += reALL[j + 3] * reB[j + 3] - imALL[j + 3] * imB[j + 3];
							imLeft[j + 0] += reALL[j + 0] * imB[j + 0] + imALL[j + 0] * reB[j + 0];
							imLeft[j + 1] += reALL[j + 1] * imB[j + 1] + imALL[j + 1] * reB[j + 1];
							imLeft[j + 2] += reALL[j + 2] * imB[j + 2] + imALL[j + 2] * reB[j + 2];
							imLeft[j + 3] += reALL[j + 3] * imB[j + 3] + imALL[j + 3] * reB[j + 3];
							reRight[j + 0] += reARR[j + 0] * reB[j + 0] - imARR[j + 0] * imB[j + 0];
							reRight[j + 1] += reARR[j + 1] * reB[j + 1] - imARR[j + 1] * imB[j + 1];
							reRight[j + 2] += reARR[j + 2] * reB[j + 2] - imARR[j + 2] * imB[j + 2];
							reRight[j + 3] += reARR[j + 3] * reB[j + 3] - imARR[j + 3] * imB[j + 3];
							imRight[j + 0] += reARR[j + 0] * imB[j + 0] + imARR[j + 0] * reB[j + 0];
							imRight[j + 1] += reARR[j + 1] * imB[j + 1] + imARR[j + 1] * reB[j + 1];
							imRight[j + 2] += reARR[j + 2] * imB[j + 2] + imARR[j + 2] * reB[j + 2];
							imRight[j + 3] += reARR[j + 3] * imB[j + 3] + imARR[j + 3] * reB[j + 3];
						}
						reLeft[end4] += reALL[end4] * reB[end4] - imALL[end4] * imB[end4];
						imLeft[end4] += reALL[end4] * imB[end4] + imALL[end4] * reB[end4];
						reRight[end4] += reARR[end4] * reB[end4] - imARR[end4] * imB[end4];
						imRight[end4] += reARR[end4] * imB[end4] + imARR[end4] * reB[end4];
					}
				}
			}
			reALL = conv->_headConvolver._segmentsLLIRRe[0];
			imALL = conv->_headConvolver._segmentsLLIRIm[0];
			reARR = conv->_headConvolver._segmentsRRIRRe[0];
			imARR = conv->_headConvolver._segmentsRRIRIm[0];
			reB = conv->_headConvolver._segmentsRe[conv->_headConvolver._current];
			imB = conv->_headConvolver._segmentsIm[conv->_headConvolver._current];
			const float *src1Re = conv->_headConvolver._preMultiplied[0][0];
			const float *src1Im = conv->_headConvolver._preMultiplied[0][1];
			const float *src2Re = conv->_headConvolver._preMultiplied[1][0];
			const float *src2Im = conv->_headConvolver._preMultiplied[1][1];
			float realL, imagL, realR, imagR;
			conv->_headConvolver._fftBuffer[0][0] = (reALL[0] * reB[0]) + src1Re[0];
			conv->_headConvolver._fftBuffer[1][0] = (reARR[0] * reB[0]) + src2Re[0];
			for (j = 1; j < conv->_headConvolver._fftComplexSize; ++j)
			{
				symIdx = conv->_headConvolver._segSize - j;
				realL = (reALL[j] * reB[j] - imALL[j] * imB[j]) + src1Re[j];
				imagL = (reALL[j] * imB[j] + imALL[j] * reB[j]) + src1Im[j];
				realR = (reARR[j] * reB[j] - imARR[j] * imB[j]) + src2Re[j];
				imagR = (reARR[j] * imB[j] + imARR[j] * reB[j]) + src2Im[j];
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[j]] = (realL + imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[0][conv->_headConvolver.bit[symIdx]] = (realL - imagL) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[j]] = (realR + imagR) * 0.5f;
				conv->_headConvolver._fftBuffer[1][conv->_headConvolver.bit[symIdx]] = (realR - imagR) * 0.5f;
			}
			// Backward FFT
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[0], conv->_headConvolver.sine);
			conv->_headConvolver.fft(conv->_headConvolver._fftBuffer[1], conv->_headConvolver.sine);

			// Add overlap
			float *result1 = y1 + processed;
			const float *a1 = conv->_headConvolver._fftBuffer[0] + inputBufferPos;
			const float *b1 = conv->_headConvolver._overlap[0] + inputBufferPos;
			float *result2 = y2 + processed;
			const float *a2 = conv->_headConvolver._fftBuffer[1] + inputBufferPos;
			const float *b2 = conv->_headConvolver._overlap[1] + inputBufferPos;
			end4 = (processing >> 2) << 2;
			for (j = 0; j < end4; j += 4)
			{
				result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->_headConvolver.gain;
				result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->_headConvolver.gain;
				result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->_headConvolver.gain;
				result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->_headConvolver.gain;
				result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->_headConvolver.gain;
				result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->_headConvolver.gain;
				result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->_headConvolver.gain;
				result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->_headConvolver.gain;
			}
			for (j = end4; j < processing; ++j)
			{
				result1[j] = (a1[j] + b1[j]) * conv->_headConvolver.gain;
				result2[j] = (a2[j] + b2[j]) * conv->_headConvolver.gain;
			}

			// Input buffer full => Next block
			conv->_headConvolver._inputBufferFill += processing;
			if (conv->_headConvolver._inputBufferFill == conv->_headConvolver._blockSize)
			{
				// Input buffer is empty again now
				memset(conv->_headConvolver._inputBuffer, 0, conv->_headConvolver._blockSize * sizeof(float));
				conv->_headConvolver._inputBufferFill = 0;
				// Save the overlap
				memcpy(conv->_headConvolver._overlap[0], conv->_headConvolver._fftBuffer[0] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				memcpy(conv->_headConvolver._overlap[1], conv->_headConvolver._fftBuffer[1] + conv->_headConvolver._blockSize, conv->_headConvolver._blockSize * sizeof(float));
				// Update current segment
				conv->_headConvolver._current = (conv->_headConvolver._current > 0) ? (conv->_headConvolver._current - 1) : conv->_headConvolver._segCountMinus1;
			}
			// Sum head and tail
			const unsigned int sumBegin = processed;
			const unsigned int sumEnd = processed + processing;
			// Sum: 1st tail block
			if (conv->_tailPrecalculated0[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated0[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated0[1][precalculatedPos];
					++precalculatedPos;
				}
			}
			// Sum: 2nd-Nth tail block
			if (conv->_tailPrecalculated[0])
			{
				unsigned int precalculatedPos = conv->_precalculatedPos;
				for (i = sumBegin; i < sumEnd; ++i)
				{
					y1[i] += conv->_tailPrecalculated[0][precalculatedPos];
					y2[i] += conv->_tailPrecalculated[1][precalculatedPos];
					++precalculatedPos;
				}
			}

			conv->_precalculatedPos += processing;

			// Convolution: 1st tail block
			if (conv->_tailPrecalculated0[0] && conv->_tailInputFill % conv->_headBlockSize == 0)
			{
				const unsigned int blockOffset = conv->_tailInputFill - conv->_headBlockSize;
				FFTConvolver1x2Process(&conv->_tailConvolver0, conv->_tailInput + blockOffset, conv->_tailOutput0[0] + blockOffset, conv->_tailOutput0[1] + blockOffset, conv->_headBlockSize);
				if (conv->_tailInputFill == conv->_tailBlockSize)
				{
					float *tmp = conv->_tailOutput0[0];
					conv->_tailOutput0[0] = conv->_tailPrecalculated0[0];
					conv->_tailPrecalculated0[0] = tmp;
					tmp = conv->_tailOutput0[1];
					conv->_tailOutput0[1] = conv->_tailPrecalculated0[1];
					conv->_tailPrecalculated0[1] = tmp;
				}
			}

			// Convolution: 2nd-Nth tail block (might be done in some background thread)
			if (conv->_tailPrecalculated[0] && conv->_tailInputFill == conv->_tailBlockSize && conv->_backgroundProcessingInput)
			{
#ifdef THREAD
				if (conv->shared_info.state == WORKING)
					task_wait1x2(conv);
				//        SampleBuffer::Swap(_tailPrecalculated, _tailOutput);
				float *tmp = conv->_tailOutput[0];
				conv->shared_info._tailOutput[0] = conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->shared_info._tailOutput[1] = conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#else
				float *tmp = conv->_tailOutput[0];
				conv->_tailOutput[0] = conv->_tailPrecalculated[0];
				conv->_tailPrecalculated[0] = tmp;
				tmp = conv->_tailOutput[1];
				conv->_tailOutput[1] = conv->_tailPrecalculated[1];
				conv->_tailPrecalculated[1] = tmp;
#endif
				memcpy(conv->_backgroundProcessingInput, conv->_tailInput, conv->_tailBlockSize * sizeof(float));
#ifdef THREAD
				task_start1x2(conv);
#else
				FFTConvolver1x2Process(&conv->_tailConvolver, conv->_backgroundProcessingInput, conv->_tailOutput[0], conv->_tailOutput[1], conv->_tailBlockSize);
#endif
			}
			if (conv->_tailInputFill == conv->_tailBlockSize)
			{
				conv->_tailInputFill = 0;
				conv->_precalculatedPos = 0;
			}
			processed += processing;
		}
	}
	else
	{
		FFTConvolver1x2Process(&conv->_headConvolver, x, y1, y2, len);
	}
}
