/*==============================================================================
Copyright (c) 2016,2018 PTC Inc. All Rights Reserved.

Copyright (c) 2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.

\file
    DeviceTrackableResult.h

\brief
    Header file for DeviceTrackableResult class. 
==============================================================================*/

#ifndef _VUFORIA_DEVICE_TRACKABLE_RESULT_H_
#define _VUFORIA_DEVICE_TRACKABLE_RESULT_H_

// Include files
#include <Vuforia/TrackableResult.h>
#include <Vuforia/DeviceTrackable.h>

namespace Vuforia
{

/// Tracking data generated by a DeviceTracker.
class VUFORIA_API DeviceTrackableResult : public TrackableResult
{
public:

    /// Get the Type for class 'DeviceTrackableResult'.
    static Type getClassType();

    /// Get the DeviceTrackable that participated in the generation of this result.
    virtual const DeviceTrackable& getTrackable() const = 0;
};

} // namespace Vuforia

#endif //_VUFORIA_DEVICE_TRACKABLE_RESULT_H_
