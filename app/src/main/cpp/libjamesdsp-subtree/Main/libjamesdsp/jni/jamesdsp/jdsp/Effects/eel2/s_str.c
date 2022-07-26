#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>
#include "s_str.h"
#define access_struct(pointer_to_void)      (*(struct S_STRING_STRUCT *) pointer_to_void)
/* This struct is what the s_str points to. This struct contains information that should not be accessed by the user. This information is vital 
 * in the s_str's implementation.
 */
struct S_STRING_STRUCT{
    // Total number of elements the string can hold before needing to reallocate
    size_t capacity;
    // Total number of elements currently in the string (the default terminating null character is included)
    size_t size;
    // Flexible array member that holds all elements
    char string[];
};
/* Creates a temporary s_str whose capacity is equal to the given number of elements plus an additional 50% of unused memory and size is 
 * equal to the given number of elements. The s_str's elements are left to be handled by the calling function.
 *
 * Returns NULL if the given number of elements is less than the size of the s_str or if the temporary s_str cannot
 * allocate the required amount of memory. Returns the temporary s_str otherwise.
 *
 * Upon success, the returned s_str MUST be destroyed at some point!
 */
static inline s_str s_str_create_temp(const s_str *const this_, const size_t num_elem){
    if(num_elem >= access_struct(*this_).size){
        s_str temp = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * (size_t) (1.5 * num_elem));
        if(temp != NULL){
            access_struct(temp).capacity = (size_t) (1.5 * num_elem);
            access_struct(temp).size = num_elem;
            return temp;
        }
    }
    return NULL;
}
s_str s_str_create(void){
    s_str new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char));
    if(new_str != NULL){
        access_struct(new_str).capacity = 1;
        access_struct(new_str).size = 1;
        access_struct(new_str).string[0] = '\0';
    }
    return new_str;
}
s_str s_str_create_from_char(const char c){
    s_str new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * 2);
    if(new_str != NULL){
        access_struct(new_str).capacity = 2;
        access_struct(new_str).size = 2;
        access_struct(new_str).string[0] = c;
        access_struct(new_str).string[1] = '\0';
    }
    return new_str;
}
s_str s_str_create_from_multi_char(const size_t length, const char c){
    s_str new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * (length + 1));
    if(new_str != NULL){
        access_struct(new_str).capacity = length + 1;
        access_struct(new_str).size = length + 1;
        for(size_t counter = 0; counter < length; ++counter){
            access_struct(new_str).string[counter] = c;
        }
        access_struct(new_str).string[length] = '\0';
    }
    return new_str;
}
s_str s_str_create_from_c_str(const char *const c_str_ptr){
    s_str new_str; 
    if(c_str_ptr == NULL){
        new_str = s_str_create();
    }else{
		size_t length = strlen(c_str_ptr) + 1;
        new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * length);
        if(new_str != NULL){
            access_struct(new_str).capacity = length;
            access_struct(new_str).size = length;
            memcpy(access_struct(new_str).string, c_str_ptr, sizeof(char) * length);
        }
    }
    return new_str;
}
s_str s_str_create_from_c_str_0Inc(const char *const c_str_ptr, const size_t length) {
	s_str new_str;
	if (c_str_ptr == NULL) {
		new_str = s_str_create();
	}
	else {
		new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * (length + 1));
		if (new_str != NULL) {
			access_struct(new_str).capacity = length + 1;
			access_struct(new_str).size = length + 1;
			memcpy(access_struct(new_str).string, c_str_ptr, sizeof(char) * (length + 1));
		}
	}
	return new_str;
}
s_str s_str_create_from_s_str(const s_str *const s_str_ptr_for_create){
    s_str new_str;
    if(s_str_ptr_for_create == NULL){
        new_str = s_str_create();
    }else{
        new_str = malloc(sizeof(struct S_STRING_STRUCT) + sizeof(char) * access_struct(*s_str_ptr_for_create).size);
        if(new_str != NULL){
            access_struct(new_str).capacity = access_struct(*s_str_ptr_for_create).size;
            access_struct(new_str).size = access_struct(*s_str_ptr_for_create).size;
            memcpy(access_struct(new_str).string, access_struct(*s_str_ptr_for_create).string, sizeof(char) * access_struct(*s_str_ptr_for_create).size);
        }
    }
    return new_str;
}
void s_str_destroy(s_str *const this_){
    if(this_ != NULL && *this_ != NULL){
        free(*this_);
        *this_ = NULL;
    }
}
const char* s_str_c_str(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    return access_struct(*this_).string;
}
const char* s_str_c_substr(const s_str *const this_, const size_t index){
    assert(this_ != NULL && *this_ != NULL);
    if(index >= access_struct(*this_).size){
        return access_struct(*this_).string + access_struct(*this_).size - 1;
    }else{
        return access_struct(*this_).string + index;
    }
}
void s_str_assign_char(s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).capacity < 2){
        s_str temp = s_str_create_temp(this_, 2);
        if(temp != NULL){
            access_struct(temp).string[0] = c;
            access_struct(temp).string[1] = '\0';
            free(*this_);
            *this_ = temp;
        }
    }else{
        access_struct(*this_).string[0] = c;
        access_struct(*this_).string[1] = '\0';
        access_struct(*this_).size = 2;
    }
}
void s_str_assign_multi_char(s_str *const this_, const size_t length, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).capacity < length + 1){
        s_str temp = s_str_create_temp(this_, length + 1);
        if(temp != NULL){
            for(size_t counter = 0; counter < length; ++counter){
                access_struct(temp).string[counter] = c;
            }
            access_struct(temp).string[length] = '\0';
            free(*this_);
            *this_ = temp;
        }
    }else{
        for(size_t counter = 0; counter < length; ++counter){
            access_struct(*this_).string[counter] = c;
        }
        access_struct(*this_).string[length] = '\0';
        access_struct(*this_).size = length + 1;
    }
}
void s_str_assign_c_str(s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(access_struct(*this_).capacity < strlen(c_str_ptr) + 1){
        s_str temp = s_str_create_temp(this_, strlen(c_str_ptr) + 1);
        if(temp != NULL){
            memcpy(access_struct(temp).string, c_str_ptr, sizeof(char) * (strlen(c_str_ptr) + 1));
            free(*this_);
            *this_ = temp;
        }
    }else{
        memcpy(access_struct(*this_).string, c_str_ptr, sizeof(char) * (strlen(c_str_ptr) + 1));
        access_struct(*this_).size = strlen(c_str_ptr) + 1;
    }
}
void s_str_assign_s_str(s_str *const this_, const s_str *const s_str_ptr_for_assign){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_assign != NULL && *s_str_ptr_for_assign != NULL);
    if(access_struct(*this_).capacity < access_struct(*s_str_ptr_for_assign).size){
        s_str temp = s_str_create_temp(this_, access_struct(*s_str_ptr_for_assign).size);
        if(temp != NULL){
            memcpy(access_struct(temp).string, access_struct(*s_str_ptr_for_assign).string, sizeof(char) * access_struct(*s_str_ptr_for_assign).size);
            free(*this_);
            *this_ = temp;
        }
    }else{
        memcpy(access_struct(*this_).string, access_struct(*s_str_ptr_for_assign).string, sizeof(char) * access_struct(*s_str_ptr_for_assign).size);
        access_struct(*this_).size = access_struct(*s_str_ptr_for_assign).size;
    }
}
char s_str_at(const s_str *const this_, const size_t index){
    assert(this_ != NULL && *this_ != NULL);
    if(index >= access_struct(*this_).size){
        return '\0';
    }else{
        return access_struct(*this_).string[index];
    }
}
void s_str_remove_at(const s_str *const this_, const size_t index){
    assert(this_ != NULL && *this_ != NULL);
    if(index < access_struct(*this_).size - 1){
        memmove(access_struct(*this_).string + index, access_struct(*this_).string + index + 1, sizeof(char) * (access_struct(*this_).size - (index + 1)));
        --access_struct(*this_).size;
    }
}
size_t s_str_find_char(const s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    for(size_t counter = 0; counter < access_struct(*this_).size; ++counter){
        if(access_struct(*this_).string[counter] == c){
            return counter;
        }
    }
    return access_struct(*this_).size - 1;
}
size_t s_str_find_c_str(const s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(strlen(c_str_ptr) == 0){
        return s_str_find_char(this_, '\0');
    }else if(strlen(c_str_ptr) < access_struct(*this_).size){
        for(size_t counter = 0; counter < access_struct(*this_).size - strlen(c_str_ptr); ++counter){
            if(memcmp(access_struct(*this_).string + counter, c_str_ptr, sizeof(char) * strlen(c_str_ptr)) == 0){
                return counter;
            }
        }
    }
    return access_struct(*this_).size - 1;
}
size_t s_str_find_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_find){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_find != NULL && *s_str_ptr_for_find != NULL);
    if(access_struct(*s_str_ptr_for_find).size == 1){
        return s_str_find_char(this_, '\0');
    }else if(access_struct(*s_str_ptr_for_find).size - 1 < access_struct(*this_).size){
        for(size_t counter = 0; counter < access_struct(*this_).size - (access_struct(*s_str_ptr_for_find).size - 1); ++counter){
            if(memcmp(access_struct(*this_).string + counter, access_struct(*s_str_ptr_for_find).string, 
                sizeof(char) * (access_struct(*s_str_ptr_for_find).size - 1)) == 0){
                return counter;
            }
        }
    }
    return access_struct(*this_).size - 1;
}
size_t s_str_rfind_char(const s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    for(size_t counter = access_struct(*this_).size - 1; ; --counter){
        if(access_struct(*this_).string[counter] == c){
            return counter;
        }else if(counter == 0){
            return access_struct(*this_).size - 1;
        }
    }
}
size_t s_str_rfind_c_str(const s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(strlen(c_str_ptr) == 0){
        return s_str_rfind_char(this_, '\0');
    }else if(strlen(c_str_ptr) < access_struct(*this_).size){
        for(size_t counter = access_struct(*this_).size - 1 - strlen(c_str_ptr); ; --counter){
            if(memcmp(access_struct(*this_).string + counter, c_str_ptr, sizeof(char) * strlen(c_str_ptr)) == 0){
                return counter;
            }else if(counter == 0){
                return access_struct(*this_).size - 1;
            }
        }
    }
    return access_struct(*this_).size - 1;
}
size_t s_str_rfind_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_rfind){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_rfind != NULL && *s_str_ptr_for_rfind != NULL);
    if(access_struct(*s_str_ptr_for_rfind).size == 1){
        return s_str_rfind_char(this_, '\0');
    }else if(access_struct(*s_str_ptr_for_rfind).size - 1 < access_struct(*this_).size){
        for(size_t counter = access_struct(*this_).size - access_struct(*s_str_ptr_for_rfind).size; ; --counter){
            if(memcmp(access_struct(*this_).string + counter, access_struct(*s_str_ptr_for_rfind).string, 
                sizeof(char) * (access_struct(*s_str_ptr_for_rfind).size - 1)) == 0){
                return counter;
            }else if(counter == 0){
                return access_struct(*this_).size - 1;
            }
        }
    }
    return access_struct(*this_).size - 1;
}
size_t s_str_capacity(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    return access_struct(*this_).capacity;    
}
void s_str_reserve(s_str *const this_, const size_t num_elem){
    assert(this_ != NULL && *this_ != NULL);
    if(num_elem > access_struct(*this_).size){
        s_str temp = realloc(*this_, sizeof(struct S_STRING_STRUCT) + sizeof(char) * num_elem);
        if(temp != NULL){
            access_struct(temp).capacity = num_elem;
            *this_ = temp;
        }
    }
}
size_t s_str_size(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    return access_struct(*this_).size;
}
size_t s_str_length(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    return strlen(access_struct(*this_).string);
}
int32_t s_str_empty(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size == 1){
        return 1;
    }else{
        return 0;
    }
}
void s_str_clear(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    access_struct(*this_).string[0] = '\0';
    access_struct(*this_).size = 1;
}
void s_str_trim(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    // A s_str will ALWAYS have at least a terminating null character; therefore, it will ALWAYS have at least a size of 1
    while(access_struct(*this_).string[0] == ' '){
        s_str_remove_at(this_, 0);
    }
    while(access_struct(*this_).size > 1 && access_struct(*this_).string[access_struct(*this_).size - 2] == ' '){
        s_str_remove_at(this_, access_struct(*this_).size  - 2);
    }
}
void s_str_shrink_to_fit(s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).capacity != access_struct(*this_).size){
        s_str temp = realloc(*this_, sizeof(struct S_STRING_STRUCT) + sizeof(char) * access_struct(*this_).size);
        if(temp != NULL){
            access_struct(temp).capacity = access_struct(temp).size;
            *this_ = temp;
        }
    }
}
void s_str_push_back(s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size + 1 > access_struct(*this_).capacity){
        s_str temp = s_str_create_temp(this_, access_struct(*this_).size + 1);
        if(temp != NULL){
            memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * (access_struct(*this_).size - 1));
            access_struct(temp).string[access_struct(*this_).size - 1] = c;
            access_struct(temp).string[access_struct(*this_).size] = '\0';
            free(*this_);
            *this_ = temp;
        }
    }else{
        access_struct(*this_).string[access_struct(*this_).size - 1] = c;
        access_struct(*this_).string[access_struct(*this_).size] = '\0';
        ++access_struct(*this_).size;
    }
}
char s_str_pop_back(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size > 1){
        char popped_char = access_struct(*this_).string[access_struct(*this_).size - 2];
        access_struct(*this_).string[access_struct(*this_).size - 2] = '\0';
        --access_struct(*this_).size;
        return popped_char;
    }
    return '\0';
}
void s_str_append_c_str(s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(access_struct(*this_).size + strlen(c_str_ptr) > access_struct(*this_).capacity){
        s_str temp = s_str_create_temp(this_, access_struct(*this_).size + strlen(c_str_ptr));
        if(temp != NULL){
            memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * (access_struct(*this_).size - 1));
            memcpy(access_struct(temp).string + access_struct(*this_).size - 1, c_str_ptr, sizeof(char) * (strlen(c_str_ptr) + 1));
            free(*this_);
            *this_ = temp;
        }
    }else{
        memcpy(access_struct(*this_).string + access_struct(*this_).size - 1, c_str_ptr, sizeof(char) * (strlen(c_str_ptr) + 1));
        access_struct(*this_).size += strlen(c_str_ptr);
    }
}
void s_str_append_s_str(s_str *const this_, const s_str *const s_str_ptr_for_append){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_append != NULL && *s_str_ptr_for_append != NULL);
    if(access_struct(*this_).size + access_struct(*s_str_ptr_for_append).size > access_struct(*this_).capacity){
        s_str temp = s_str_create_temp(this_, access_struct(*this_).size + access_struct(*s_str_ptr_for_append).size - 1);
        if(temp != NULL){
            memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * (access_struct(*this_).size - 1));
            memcpy(access_struct(temp).string + access_struct(*this_).size - 1, access_struct(*s_str_ptr_for_append).string,
                sizeof(char) * access_struct(*s_str_ptr_for_append).size);
            free(*this_);
            *this_ = temp;
        }
    }else{
        memcpy(access_struct(*this_).string + access_struct(*this_).size - 1, access_struct(*s_str_ptr_for_append).string, 
            sizeof(char) * access_struct(*s_str_ptr_for_append).size);
        access_struct(*this_).size += access_struct(*s_str_ptr_for_append).size - 1;
    }
}
const char* s_str_first_word(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    return access_struct(*this_).string;
}
void s_str_remove_first_word(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size > 1){
        const size_t index = s_str_find_char(this_, ' ');
        if(index == access_struct(*this_).size - 1){
            s_str_clear(this_);
        }else{
            memmove(access_struct(*this_).string, access_struct(*this_).string + index + 1, sizeof(char) * (access_struct(*this_).size - (index + 1)));
            access_struct(*this_).size -= index + 1;
        }
    }
}
const char* s_str_last_word(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    const size_t index = s_str_rfind_char(this_, ' ');
    if(index == access_struct(*this_).size - 1 || access_struct(*this_).size == 2){
        return access_struct(*this_).string;
    }else{
        return access_struct(*this_).string + index + 1; 
    }
}
void s_str_remove_last_word(const s_str *const this_){
    assert(this_ != NULL && *this_ != NULL);
    const size_t index = s_str_rfind_char(this_, ' ');
    if(index == access_struct(*this_).size - 1){
        s_str_clear(this_);
    }else{
        access_struct(*this_).string[index] = '\0';
        access_struct(*this_).size -= access_struct(*this_).size - (index + 1);
    }
}
void s_str_erase(const s_str *const this_, const size_t index, size_t length){
    assert(this_ != NULL && *this_ != NULL);
    if(index < access_struct(*this_).size - 1){
        if(index + length >= access_struct(*this_).size - 1){
            length = access_struct(*this_).size - (index + 1);
        }
        memmove(access_struct(*this_).string + index, access_struct(*this_).string + index + length, 
            sizeof(char) * (access_struct(*this_).size - (index + length)));
        access_struct(*this_).size -= length;
    }
}
void s_str_insert_char(s_str *const this_, const size_t index, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(index < access_struct(*this_).size){
        if(access_struct(*this_).size + 1 > access_struct(*this_).capacity){
            s_str temp = s_str_create_temp(this_, access_struct(*this_).size + 1);
            if(temp != NULL){
                memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * index);
                memcpy(access_struct(temp).string + index + 1, access_struct(*this_).string + index, 
                    sizeof(char) * (access_struct(*this_).size - index));
                access_struct(temp).string[index] = c;
                free(*this_);
                *this_ = temp;
            }
        }else{
            memmove(access_struct(*this_).string + index + 1, access_struct(*this_).string + index, 
                sizeof(char) * (access_struct(*this_).size - index));
            access_struct(*this_).string[index] = c;
            ++access_struct(*this_).size;
        }
    }
}
void s_str_insert_c_str(s_str *const this_, const size_t index, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(index < access_struct(*this_).size && strlen(c_str_ptr) > 0){
        if(access_struct(*this_).size + strlen(c_str_ptr) > access_struct(*this_).capacity){
            s_str temp = s_str_create_temp(this_, access_struct(*this_).size + strlen(c_str_ptr));
            if(temp != NULL){
                memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * index);
                memcpy(access_struct(temp).string + index + strlen(c_str_ptr), access_struct(*this_).string + index,
                    sizeof(char) * (access_struct(*this_).size - index));
                memcpy(access_struct(temp).string + index, c_str_ptr, sizeof(char) * strlen(c_str_ptr));
                free(*this_);
                *this_ = temp;
            }
        }else{
            memmove(access_struct(*this_).string + index + strlen(c_str_ptr), access_struct(*this_).string + index,
                sizeof(char) * (access_struct(*this_).size - index));
            memcpy(access_struct(*this_).string + index, c_str_ptr, sizeof(char) * strlen(c_str_ptr));
            access_struct(*this_).size += strlen(c_str_ptr);
        }
    }
}
void s_str_insert_s_str(s_str *const this_, const size_t index, const s_str *const s_str_ptr_for_insert){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_insert != NULL && *s_str_ptr_for_insert != NULL);
    if(index < access_struct(*this_).size && access_struct(*s_str_ptr_for_insert).size - 1 > 0){
        if(access_struct(*this_).size + access_struct(*s_str_ptr_for_insert).size > access_struct(*this_).capacity){
            s_str temp = s_str_create_temp(this_, access_struct(*this_).size + access_struct(*s_str_ptr_for_insert).size - 1);
            if(temp != NULL){
                memcpy(access_struct(temp).string, access_struct(*this_).string, sizeof(char) * index);
                memcpy(access_struct(temp).string + index + access_struct(*s_str_ptr_for_insert).size - 1, access_struct(*this_).string + index,
                    sizeof(char) * (access_struct(*this_).size - index));
                memcpy(access_struct(temp).string + index, access_struct(*s_str_ptr_for_insert).string, 
                    sizeof(char) * (access_struct(*s_str_ptr_for_insert).size - 1));
                free(*this_);
                *this_ = temp;
            }
        }else{
            memmove(access_struct(*this_).string + index + access_struct(*s_str_ptr_for_insert).size - 1, access_struct(*this_).string + index,
                sizeof(char) * (access_struct(*this_).size - index));
            memcpy(access_struct(*this_).string + index, access_struct(*s_str_ptr_for_insert).string, 
                sizeof(char) * (access_struct(*s_str_ptr_for_insert).size - 1));
            access_struct(*this_).size += access_struct(*s_str_ptr_for_insert).size - 1;
        }
    }
}
void s_str_sepwith_char(s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size > 1){
        char *const slate = malloc(sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * 1));
        if(slate != NULL){
            for(size_t num_iterations = 0, counter = 0; num_iterations < access_struct(*this_).size - 2; ++num_iterations, counter += 2){
                slate[counter] = access_struct(*this_).string[num_iterations];
                slate[counter + 1] = c;
            }
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * 1 - 2] = access_struct(*this_).string[access_struct(*this_).size - 2];
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * 1 - 1] = '\0';
            if(access_struct(*this_).size + (access_struct(*this_).size - 2) * 1 > access_struct(*this_).capacity){
                s_str temp = s_str_create_temp(this_, access_struct(*this_).size + (access_struct(*this_).size - 2) * 1);
                if(temp != NULL){
                    memcpy(access_struct(temp).string, slate, sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * 1));
                    free(*this_);
                    *this_ = temp;
                }
            }else{
                memcpy(access_struct(*this_).string, slate, sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * 1));
                access_struct(*this_).size += (access_struct(*this_).size - 2) * 1;
            }
            free(slate);
        }
    }
}
void s_str_sepwith_c_str(s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(access_struct(*this_).size > 1 && strlen(c_str_ptr) > 0){
        char *const slate = malloc(sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr)));
        if(slate != NULL){
            for(size_t num_iterations = 0, counter = 0; num_iterations < access_struct(*this_).size - 2; ++num_iterations, counter += 1 + strlen(c_str_ptr)){
                slate[counter] = access_struct(*this_).string[num_iterations];
                memcpy(slate + counter + 1, c_str_ptr, sizeof(char) * strlen(c_str_ptr));
            }
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr) - 2] = access_struct(*this_).string[access_struct(*this_).size - 2];
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr) - 1] = '\0';
            if(access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr) > access_struct(*this_).capacity){
                s_str temp = s_str_create_temp(this_, access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr));
                if(temp != NULL){
                    memcpy(access_struct(temp).string, slate, 
                        sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr)));
                    free(*this_);
                    *this_ = temp;
                }
            }else{
                memcpy(access_struct(*this_).string, slate, 
                    sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * strlen(c_str_ptr)));
                access_struct(*this_).size += (access_struct(*this_).size - 2) * strlen(c_str_ptr);
            }
            free(slate);
        }
    }
}
void s_str_sepwith_s_str(s_str *const this_, const s_str *const s_str_ptr_for_sepwith){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_sepwith != NULL && *s_str_ptr_for_sepwith != NULL);
    if(access_struct(*this_).size > 1 && access_struct(*s_str_ptr_for_sepwith).size > 1){
        char *const slate = malloc(sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * 
            (access_struct(*s_str_ptr_for_sepwith).size - 1)));
        if(slate != NULL){
            for(size_t num_iterations = 0, counter = 0; num_iterations < access_struct(*this_).size - 2; 
                ++num_iterations, counter += access_struct(*s_str_ptr_for_sepwith).size){
                slate[counter] = access_struct(*this_).string[num_iterations];
                memcpy(slate + counter + 1, access_struct(*s_str_ptr_for_sepwith).string, sizeof(char) * (access_struct(*s_str_ptr_for_sepwith).size - 1));
            }
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1) - 2] = 
                access_struct(*this_).string[access_struct(*this_).size - 2];
            slate[access_struct(*this_).size + (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1) - 1] = '\0';
            if(access_struct(*this_).size + (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1) > access_struct(*this_).capacity){
                s_str temp = s_str_create_temp(this_, access_struct(*this_).size + (access_struct(*this_).size - 2) * 
                    (access_struct(*s_str_ptr_for_sepwith).size - 1));
                if(temp != NULL){
                    memcpy(access_struct(temp).string, slate,
                        sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1)));
                    free(*this_);
                    *this_ = temp;
                }
            }else{
                memcpy(access_struct(*this_).string, slate,
                    sizeof(char) * (access_struct(*this_).size + (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1)));
                access_struct(*this_).size += (access_struct(*this_).size - 2) * (access_struct(*s_str_ptr_for_sepwith).size - 1);
            }
            free(slate);
        }
    }
}
void s_str_replace(const s_str *const this_, const size_t index, size_t length, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(index < access_struct(*this_).size - 1){
        if(index + length >= access_struct(*this_).size - 1){
            length = access_struct(*this_).size - (index + 1);
        }
        memset(access_struct(*this_).string + index, c, sizeof(char) * length);
    }
}
void s_str_replace_all(const s_str *const this_, const char c){
    assert(this_ != NULL && *this_ != NULL);
    if(access_struct(*this_).size > 1){
        memset(access_struct(*this_).string, c, sizeof(char) * (access_struct(*this_).size - 1));
    }
}
void s_str_swap(s_str *const this_, s_str *const s_str_ptr_for_swap){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_swap != NULL && *s_str_ptr_for_swap != NULL);
    s_str temp = *this_;
    *this_ = *s_str_ptr_for_swap;
    *s_str_ptr_for_swap = temp;
}
void s_str_foreach(const s_str *this_, const size_t index, size_t length, void (*const fptr)(char *)){
    assert(this_ != NULL && *this_ != NULL && fptr != NULL);
    if(index < access_struct(*this_).size - 1){
        if(index + length >= access_struct(*this_).size - 1){
            length = access_struct(*this_).size - (index + 1);
        }
        for(size_t counter = index; counter < index + length; ++counter){
            fptr(access_struct(*this_).string + counter);
        }
    }
}
void s_str_foreach_all(const s_str *this_, void (*const fptr)(char *)){
    assert(this_ != NULL && *this_ != NULL && fptr != NULL);
    for(size_t counter = 0; counter < access_struct(*this_).size - 1; ++counter){
        fptr(access_struct(*this_).string + counter);
    }
}
int32_t s_str_compare_c_str(const s_str *const this_, const char *const c_str_ptr){
    assert(this_ != NULL && *this_ != NULL && c_str_ptr != NULL);
    if(access_struct(*this_).size - 1 > strlen(c_str_ptr)){
        const int32_t compared_val = memcmp(access_struct(*this_).string, c_str_ptr, sizeof(char) * strlen(c_str_ptr));
        if(compared_val == 0){
            return 1;
        }else{
            return compared_val;
        }
    }else if(access_struct(*this_).size - 1 < strlen(c_str_ptr)){
        const int32_t compared_val = memcmp(access_struct(*this_).string, c_str_ptr, sizeof(char) * (access_struct(*this_).size - 1));
        if(compared_val == 0){
            return -1;
        }else{
            return compared_val;
        }
    }else{
        const int32_t compared_val = memcmp(access_struct(*this_).string, c_str_ptr, sizeof(char) * (access_struct(*this_).size - 1));
        return compared_val;
    }
}
int32_t s_str_compare_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_compare){
    assert(this_ != NULL && *this_ != NULL && s_str_ptr_for_compare != NULL && *s_str_ptr_for_compare != NULL);
    if(access_struct(*this_).size - 1 > access_struct(*s_str_ptr_for_compare).size - 1){
        const int32_t compared_val = memcmp(access_struct(*this_).string, access_struct(*s_str_ptr_for_compare).string, 
            sizeof(char) * (access_struct(*s_str_ptr_for_compare).size - 1));
        if(compared_val == 0){
            return 1;
        }else{
            return compared_val;
        }
    }else if(access_struct(*this_).size - 1 < access_struct(*s_str_ptr_for_compare).size - 1){
        const int32_t compared_val = memcmp(access_struct(*this_).string, access_struct(*s_str_ptr_for_compare).string, 
            sizeof(char) * (access_struct(*this_).size - 1));
        if(compared_val == 0){
            return -1;
        }else{
            return compared_val;
        }
    }else{
        const int32_t compared_val = memcmp(access_struct(*this_).string, access_struct(*s_str_ptr_for_compare).string, 
            sizeof(char) * (access_struct(*this_).size - 1));
        return compared_val;
    }
}
void s_str_sort(s_str *const this_, int32_t (*const compare)(const void *, const void *)){
    assert(this_ != NULL && *this_ != NULL && compare != NULL);
    if(access_struct(*this_).size > 2){
        qsort(access_struct(*this_).string, access_struct(*this_).size - 1, sizeof(char), compare);
    }
}