//
// Created on 2025/11/10.
//

#ifndef AUDIOPROCESSORSAMPLE_BASS_BOOST_H
#define AUDIOPROCESSORSAMPLE_BASS_BOOST_H

#include <cstring>
#include <malloc.h>

typedef struct{
    float a[2];
    float b[3];
    float history_x[2];
    float history_y[2];
}biquad_t;

typedef struct{
    biquad_t left;
    biquad_t right;
}bass_boost_filter_t;

void *initBassBoost();
void setBassBoostParam(void *inst, float gain, float Q, float freq, int sample_rate);
int processBassBoost(void *inst, int16_t *input_data, int16_t *output_data, int input_len);
int processBassBoostFloat(void *inst, float *input_data, float *output_data, int input_len);
void closeBassBoost(void *inst);

#endif //AUDIOPROCESSORSAMPLE_BASS_BOOST_H
