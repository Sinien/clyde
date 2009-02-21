//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.ListUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ConditionConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

import static com.threerings.tudey.Log.*;

/**
 * Handles the evaluation of conditions.
 */
public abstract class ConditionLogic extends Logic
{
    /**
     * Evaluates the tagged condition.
     */
    public static class Tagged extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            String tag = ((ConditionConfig.Tagged)_config).tag;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (ListUtil.contains(_targets.get(ii).getTags(), tag)) {
                        return true;
                    }
                }
                return false;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.Tagged)_config).target, _source);
        }

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the instance of condition.
     */
    public static class InstanceOf extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (_logicClass.isInstance(_targets.get(ii))) {
                        return true;
                    }
                }
                return false;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.InstanceOf config = (ConditionConfig.InstanceOf)_config;
            try {
                _logicClass = Class.forName(config.logicClass);
            } catch (ClassNotFoundException e) {
                log.warning("Missing logic class for InstanceOf condition.", e);
                _logicClass = Logic.class;
            }
            _target = createTarget(config.target, _source);
        }

        /** The test class. */
        protected Class<?> _logicClass;

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the intersect condition logic.
     */
    public static class Intersecting extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    Shape s1 = _firsts.get(ii);
                    for (int jj = 0, mm = _seconds.size(); jj < mm; jj++) {
                        Shape s2 = _seconds.get(jj);
                        if (s1.intersects(s2)) {
                            return true;
                        }
                    }
                }
                return false;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.Intersecting config = (ConditionConfig.Intersecting)_config;
            _first = createRegion(config.first, _source);
            _second = createRegion(config.second, _source);
        }

        /** The regions to check. */
        protected RegionLogic _first, _second;

        /** Holds shapes during evaluation. */
        protected ArrayList<Shape> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the distance within condition logic.
     */
    public static class DistanceWithin extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    Vector2f t1 = _firsts.get(ii).getTranslation();
                    for (int jj = 0, mm = _seconds.size(); jj < mm; jj++) {
                        Vector2f t2 = _seconds.get(jj).getTranslation();
                        if (FloatMath.isWithin(t1.distance(t2), config.minimum, config.maximum)) {
                            return true;
                        }
                    }
                }
                return false;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            _first = createTarget(config.first, _source);
            _second = createTarget(config.second, _source);
        }

        /** The targets to check. */
        protected TargetLogic _first, _second;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the all condition.
     */
    public static class All extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (!condition.isSatisfied(activator)) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.All)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Evaluates the any condition.
     */
    public static class Any extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (condition.isSatisfied(activator)) {
                    return true;
                }
            }
            return false;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.All)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ConditionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Determines whether the condition is satisfied.
     *
     * @param activator the entity that triggered the action.
     */
    public abstract boolean isSatisfied (Logic activator);

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The condition configuration. */
    protected ConditionConfig _config;

    /** The action source. */
    protected Logic _source;
}