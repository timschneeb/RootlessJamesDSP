/*
  Expression Evaluator Library (NS-EEL) v2
  Copyright (C) 2004-2013 Cockos Incorporated
  Copyright (C) 1999-2003 Nullsoft, Inc.
  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.
  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:
  1. The origin of this software must not be misrepresented; you must not
	 claim that you wrote the original software. If you use this software
	 in a product, an acknowledgment in the product documentation would be
	 appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
	 misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.
*/
#include "eelCommon.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#ifdef _WIN32
#include <malloc.h>
#ifdef _MSC_VER
#define inline __inline
#endif
#endif
float * NSEEL_CGEN_CALL __NSEEL_RAM_MemCpy(float *blocks, float *dest, float *src, float *lenptr)
{
	int32_t dest_offs = (int32_t)(*dest + 0.0001f);
	int32_t src_offs = (int32_t)(*src + 0.0001f);
	int32_t len = (int32_t)(*lenptr + 0.0001f);
	int32_t want_mmove = 0;
	// trim to front
	if (src_offs < 0)
	{
		len += src_offs;
		dest_offs -= src_offs;
		src_offs = 0;
	}
	if (dest_offs < 0)
	{
		len += dest_offs;
		src_offs -= dest_offs;
		dest_offs = 0;
	}
	if (src_offs + len > NSEEL_RAM_ITEMSPERBLOCK) len = NSEEL_RAM_ITEMSPERBLOCK - src_offs;
	if (dest_offs + len > NSEEL_RAM_ITEMSPERBLOCK) len = NSEEL_RAM_ITEMSPERBLOCK - dest_offs;
	if (src_offs == dest_offs || len < 1) return dest;
	if (src_offs < dest_offs && src_offs + len > dest_offs)
	{
		// if src_offs < dest_offs and overlapping, must copy right to left
		if ((dest_offs - src_offs) < NSEEL_RAM_ITEMSPERBLOCK) want_mmove = 1;
		src_offs += len;
		dest_offs += len;
		while (len > 0)
		{
			const int32_t maxdlen = ((dest_offs - 1)&(NSEEL_RAM_ITEMSPERBLOCK - 1)) + 1;
			const int32_t maxslen = ((src_offs - 1)&(NSEEL_RAM_ITEMSPERBLOCK - 1)) + 1;
			int32_t copy_len = len;
			float *srcptr, *destptr;
			if (copy_len > maxdlen) copy_len = maxdlen;
			if (copy_len > maxslen) copy_len = maxslen;
			srcptr = __NSEEL_RAMAlloc(blocks, src_offs - copy_len);
			destptr = __NSEEL_RAMAlloc(blocks, dest_offs - copy_len);
//			if (srcptr == &nseel_ramalloc_onfail || destptr == &nseel_ramalloc_onfail) break;
			if (want_mmove) memmove(destptr, srcptr, sizeof(float)*copy_len);
			else memcpy(destptr, srcptr, sizeof(float)*copy_len);
			src_offs -= copy_len;
			dest_offs -= copy_len;
			len -= copy_len;
		}
		return dest;
	}
	if (dest_offs < src_offs && dest_offs + len > src_offs)
	{
		// if dest_offs < src_offs and overlapping, and less than NSEEL_RAM_ITEMSPERBLOCK apart, use memmove()
		if ((src_offs - dest_offs) < NSEEL_RAM_ITEMSPERBLOCK) want_mmove = 1;
	}
	while (len > 0)
	{
		const int32_t maxdlen = NSEEL_RAM_ITEMSPERBLOCK - (dest_offs&(NSEEL_RAM_ITEMSPERBLOCK - 1));
		const int32_t maxslen = NSEEL_RAM_ITEMSPERBLOCK - (src_offs&(NSEEL_RAM_ITEMSPERBLOCK - 1));
		int32_t copy_len = len;
		float *srcptr, *destptr;
		if (copy_len > maxdlen) copy_len = maxdlen;
		if (copy_len > maxslen) copy_len = maxslen;
		srcptr = __NSEEL_RAMAlloc(blocks, src_offs);
		destptr = __NSEEL_RAMAlloc(blocks, dest_offs);
//		if (srcptr == &nseel_ramalloc_onfail || destptr == &nseel_ramalloc_onfail) break;
		if (want_mmove) memmove(destptr, srcptr, sizeof(float)*copy_len);
		else memcpy(destptr, srcptr, sizeof(float)*copy_len);
		src_offs += copy_len;
		dest_offs += copy_len;
		len -= copy_len;
	}
	return dest;
}
float * NSEEL_CGEN_CALL __NSEEL_RAM_MemSet(float *blocks, float *dest, float *v, float *lenptr)
{
	int32_t offs = (int32_t)(*dest + 0.0001f);
	int32_t len = (int32_t)(*lenptr + 0.0001f);
	float t;
	if (offs < 0)
	{
		len += offs;
		offs = 0;
	}
	if (offs >= NSEEL_RAM_ITEMSPERBLOCK) return dest;
	if (offs + len > NSEEL_RAM_ITEMSPERBLOCK) len = NSEEL_RAM_ITEMSPERBLOCK - offs;
	if (len < 1) return dest;
	t = *v; // set value
  //  int32_t lastBlock=-1;
	while (len > 0)
	{
		int32_t lcnt;
		float *ptr = __NSEEL_RAMAlloc(blocks, offs);
//		if (ptr == &nseel_ramalloc_onfail) break;
		lcnt = NSEEL_RAM_ITEMSPERBLOCK - (offs&(NSEEL_RAM_ITEMSPERBLOCK - 1));
		if (lcnt > len) lcnt = len;
		len -= lcnt;
		offs += lcnt;
		while (lcnt--)
		{
			*ptr++ = t;
		}
	}
	return dest;
}