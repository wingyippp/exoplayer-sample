//
// Created on 2025/11/10.
//

#include "bass_boost.h"
#define CLAMP_INT16_SIMPLE(x) ((x) < -32768 ? -32768 : ((x) > 32767 ? 32767 : (x)))

// boost 6dB
//float bass_boost_filter_coeff_b[3] = {1.00315259e+00,-1.98468243e+00, 9.81760338e-01};
//float bass_boost_filter_coeff_a[2] = {-1.98473992e+00, 9.84855443e-01};

// boost 9dB
//float bass_boost_filter_coeff_b[3] = {1.00530646e+00,-1.98630007e+00, 9.81284028e-01};
//float bass_boost_filter_coeff_a[2] = {-1.98639936e+00, 9.86491195e-01};

// boost 12dB
float bass_boost_filter_coeff_b[3] = {1.00588750e+00,-1.98810028e+00, 9.82488082e-01};
float bass_boost_filter_coeff_a[2] = {-1.98820336e+00, 9.88272509e-01};

void *initBassBoost()
{
    auto *inst = (bass_boost_filter_t *)malloc(sizeof(bass_boost_filter_t));
    memset(inst, 0, sizeof(bass_boost_filter_t));

    memcpy(inst->left.b, bass_boost_filter_coeff_b, 3*sizeof(float));
    memcpy(inst->left.a, bass_boost_filter_coeff_a, 2*sizeof(float));
    memcpy(inst->right.b, bass_boost_filter_coeff_b, 3*sizeof(float));
    memcpy(inst->right.a, bass_boost_filter_coeff_a, 2*sizeof(float));

    return inst;
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
    iir_filter(input_data+samples_per_ch, samples_per_ch, &(process_t->right));

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
