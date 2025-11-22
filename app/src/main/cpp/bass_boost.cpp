//
// Created on 2025/11/10.
//
#include <cmath>
#include "bass_boost.h"
#define CLAMP_INT16_SIMPLE(x) ((x) < -32768 ? -32768 : ((x) > 32767 ? 32767 : (x)))
#define PI 3.141592653
#define MAX_BOOST 21

void *initBassBoost()
{
    auto *inst = (bass_boost_filter_t *)malloc(sizeof(bass_boost_filter_t));
    memset(inst, 0, sizeof(bass_boost_filter_t));

    return inst;
}

void setBassBoostParam(void *inst, float gain, float Q, float freq, int sample_rate)
{
    auto *process_t = (bass_boost_filter_t *)inst;
    double b[3], a[3];
    double w0, A, alpha;
    double cos_w0, sqrt_A;
    double inv_a0;
    gain = MAX_BOOST * gain;
    w0 = 2 * PI * freq / sample_rate;
    A = sqrt(pow(10, gain*0.05));
    alpha = sin(w0) / (2 * Q);
    cos_w0 = cos(w0);
    sqrt_A = sqrt(A);

    b[0] = A * ((A + 1) - (A - 1) * cos_w0 + 2 * sqrt_A * alpha);
    b[1] = 2 * A * ((A - 1) - (A + 1) * cos_w0);
    b[2] = A * ((A + 1) - (A - 1) * cos_w0 - 2 * sqrt_A * alpha);
    a[0] = (A + 1) + (A - 1) * cos_w0 + 2 * sqrt_A * alpha;
    a[1] = -2 * ((A - 1) + (A + 1) * cos_w0);
    a[2] = (A + 1) + (A - 1) * cos_w0 - 2 * sqrt_A * alpha;

    inv_a0 = 1.0 / a[0];
    process_t->right.b[0] = process_t->left.b[0] = float(b[0] * inv_a0);
    process_t->right.b[1] = process_t->left.b[1] = float(b[1] * inv_a0);
    process_t->right.b[2] = process_t->left.b[2] = float(b[2] * inv_a0);
    process_t->right.a[0] = process_t->left.a[0] = float(a[1] * inv_a0);
    process_t->right.a[1] = process_t->left.a[1] = float(a[2] * inv_a0);

}

void iir_filter(float *inOut, int len, biquad_t *filter)
{
    float coeff_b[3], coeff_a[2];
    float his_y[2], his_x[2];
    coeff_b[0] = filter->b[0];
    coeff_b[1] = filter->b[1];
    coeff_b[2] = filter->b[2];
    coeff_a[0] = -filter->a[0];
    coeff_a[1] = -filter->a[1];

    his_y[0] = filter->history_y[0];
    his_y[1] = filter->history_y[1];
    his_x[0] = filter->history_x[0];
    his_x[1] = filter->history_x[1];

    float acc = 0;
    for(int i = 0; i < len; i++){
        acc = coeff_b[0] * inOut[i];
        acc += coeff_b[1] * his_x[0];
        acc += coeff_b[2] * his_x[1];
        acc += coeff_a[0] * his_y[0];
        acc += coeff_a[1] * his_y[1];

        his_x[1] = his_x[0];
        his_x[0] = inOut[i];
        his_y[1] = his_y[0];
        his_y[0] = acc;

        inOut[i] = acc;
    }

    filter->history_y[0] = his_y[0];
    filter->history_y[1] = his_y[1];
    filter->history_x[0] = his_x[0];
    filter->history_x[1] = his_x[1];
}

void iir_filter_int(int16_t *inOut, int len, biquad_t *filter)
{
    float coeff_b[3], coeff_a[2];
    float his_y[2], his_x[2];
    coeff_b[0] = filter->b[0];
    coeff_b[1] = filter->b[1];
    coeff_b[2] = filter->b[2];
    coeff_a[0] = -filter->a[0];
    coeff_a[1] = -filter->a[1];

    his_y[0] = filter->history_y[0];
    his_y[1] = filter->history_y[1];
    his_x[0] = filter->history_x[0];
    his_x[1] = filter->history_x[1];

    float acc = 0;
    float input = 0;
    float res = 0;
    for(int i = 0; i < len; i++){
        input = (float)inOut[i] / INT16_MAX;
        acc = coeff_b[0] * input;
        acc += coeff_b[1] * his_x[0];
        acc += coeff_b[2] * his_x[1];
        acc += coeff_a[0] * his_y[0];
        acc += coeff_a[1] * his_y[1];

        his_x[1] = his_x[0];
        his_x[0] = input;
        his_y[1] = his_y[0];
        his_y[0] = acc;

        res = acc * INT16_MAX;
        inOut[i] = CLAMP_INT16_SIMPLE(res);
    }

    filter->history_y[0] = his_y[0];
    filter->history_y[1] = his_y[1];
    filter->history_x[0] = his_x[0];
    filter->history_x[1] = his_x[1];
}

int processBassBoost(void *inst, int16_t *input_data, int16_t *output_data, int input_len)
{
    auto *process_t = (bass_boost_filter_t *)inst;
    int samples_per_ch = input_len / 2; //for stereo audio

    memcpy(output_data, input_data, input_len*sizeof(int16_t));

    for(int i = 0; i < samples_per_ch; i++){
        input_data[i] = output_data[2 * i];
        input_data[i + samples_per_ch] = output_data[2 * i + 1];
    }

    iir_filter_int(input_data, samples_per_ch, &(process_t->left));
    iir_filter_int(input_data+samples_per_ch, samples_per_ch, &(process_t->right));

    for(int i = 0; i < samples_per_ch; i++){
        output_data[2 * i] = input_data[i];
        output_data[2 * i + 1] = input_data[i + samples_per_ch];
    }

    return 0;
}

int processBassBoost16BitToFloat(void *inst, int16_t *input_data, float *output_data, int input_len)
{
    auto *process_t = (bass_boost_filter_t *)inst;
    int samples_per_ch = input_len / 2; //for stereo audio
    auto tmp_buf = (float *)malloc(input_len * sizeof(float));

    float inv_INT16_MAX = 1.0 / 32768;
    for(int i = 0; i < samples_per_ch; i++){
        tmp_buf[i] = (float)input_data[2 * i] * inv_INT16_MAX;
        tmp_buf[i + samples_per_ch] = (float)input_data[2 * i + 1] * inv_INT16_MAX;
    }

    iir_filter(tmp_buf, samples_per_ch, &(process_t->left));
    iir_filter(tmp_buf + samples_per_ch, samples_per_ch, &(process_t->right));

    for(int i = 0; i < samples_per_ch; i++){
        output_data[2 * i] = tmp_buf[i];
        output_data[2 * i + 1] = tmp_buf[i + samples_per_ch];
    }

    if(tmp_buf){
        free(tmp_buf);
        tmp_buf = nullptr;
    }
    return 0;
}

int processBassBoostFloat(void *inst, float *input_data, float *output_data, int input_len)
{
    auto *process_t = (bass_boost_filter_t *)inst;
    int samples_per_ch = input_len / 2; //for stereo audio

    memcpy(output_data, input_data, input_len*sizeof(float));

    for(int i = 0; i < samples_per_ch; i++){
        input_data[i] = output_data[2 * i];
        input_data[i + samples_per_ch] = output_data[2 * i + 1];
    }

    iir_filter(input_data, samples_per_ch, &(process_t->left));
    iir_filter(input_data + samples_per_ch, samples_per_ch, &(process_t->right));

    for(int i = 0; i < samples_per_ch; i++){
        output_data[2 * i] = input_data[i];
        output_data[2 * i + 1] = input_data[i + samples_per_ch];
    }

    return 0;
}

void closeBassBoost(void *inst)
{
    auto *process_t = (bass_boost_filter_t *)inst;

    if(process_t){
        free(process_t);
        process_t = nullptr;
    }

}
