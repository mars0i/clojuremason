;; This software is copyright 2016, 2017, 2018, 2019 by Marshall Abrams, 
;; and is distributed under the Gnu General Public License version 3.0 
;; as specified in the the file LICENSE.

;(set! *warn-on-reflection* true)

(ns example.Sim
  (:require [clojure.tools.cli]
            [masonclj.params :as sp]
            [example.snipe :as sn]
            [example.popenv :as pe])
  (:import [sim.engine Steppable Schedule Stoppable]
           [sim.util Interval]
           [ec.util MersenneTwisterFast]
           [java.lang String]
           [example.popenv PopEnv]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generate Sim class as subclass of SimState using genclass, with an init 
;; function, import statement, and Bean/MASON field accessors.
;; To see what code will be generated, try this in a repl:
;; FIXME:
;;    (require '[utils.defsim :as defsim])
;;    (pprint (macroexpand-1 '<insert defsim call>))

(def commandline$ (atom nil)) ; Used by record-commandline-args!, which is defined by defsim, and below

;; Note: There is no option below for max number of steps.  Use MASON's -for instead.
;; Avoid the following characters for single-character options, because MASON already
;; uses them for single-dash options: c d f h p q r s t u.  Also avoid numbers, because
;; MASON allows setting '-seed <old seed number>', and old seed number may be a negative
;; number, in which case the app gets confused if I use e.g. -2 as an option below.
(sp/defparams  [;field name   initial-value type             in ui? with range?
                [num-r-snipes       25      long                    [0,500]     ["-R" "Size of r-snipe subpopulation" :parse-fn #(Long. %)]]
                [birth-threshold    20.0    double                  [1.0,50.0]  ["-b" "Energy level at which birth takes place" :parse-fn #(Double. %)]]
                [carrying-proportion 0.25   double                  [0.1,0.9]   ["-C" "Snipes are randomly culled when number exceed this times # of cells in a subenv (east or west)." :parse-fn #(Double. %)]]
                [env-width          40      long                    [10,250]    ["-W" "Width of env.  Must be an even number." :parse-fn #(Long. %)]] ; Haven't figured out how to change 
                [env-height         40      long                    [10,250]    ["-H" "Height of env. Must be an even number." :parse-fn #(Long. %)]] ;  within app without distortion
                [extreme-pref        1.0    double                  true        ["-x" "Absolute value of r-snipe preferences." :parse-fn #(Double. %)]]
                [env-display-size   12.0    double                  false       ["-G" "How large to display the env in gui by default." :parse-fn #(Double. %)]]
                [use-gui           false    boolean                 false       ["-g" "If -g, use GUI; otherwise use GUI if and only if +g or there are no commandline options." :parse-fn #(Boolean. %)]]
                [max-subenv-pop-size 0      long    false] ; maximum per-subenvironment population size
                [seed               nil     long    false] ; convenience field to store Sim's seed
                [in-gui           false     boolean false] ; convenience field to store Boolean re whether in GUI
                [popenv             nil  example.popenv.PopEnv false]
               ]
  :exposes-methods {finish superFinish} ; name for function to call finish() in the superclass
  :methods [[getPopSize [] long]] ; Signatures for Java-visible methods that won't be autogenerated, but be defined below.
  )

;; no good reason to put this into the defsim macro since it doesn't include any
;; field-specific code.  Easier to redefine if left here.
(defn set-sim-data-from-commandline!
  "Set fields in the Sim's simData from parameters passed on the command line."
  [^Sim sim cmdline$]
  (let [options (:options @cmdline$)
        sim-data (.simData sim)]
    (run! #(apply swap! sim-data assoc %) ; arg is a MapEntry, which is sequential? so will function like a list or vector
          options)))

;; Bean-style methods that will automatically result in GUI elements,
;; but that are not auto-generated by the defparams macro:
;; NOTE these get called on every tick in GUI even if not reported:
(defn -getPopSize
  [^Sim this] 
  (count (:snipe-map (:popenv @(.simData this)))))

(defn curr-step
  "Convenience function to return the current step number (or tick)."
  [^Sim sim]
  (.getSteps (.schedule sim)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (let [^"[Ljava.lang.String;" arg-array (into-array args)]
    (sim.engine.SimState/doLoop example.Sim arg-array)
    (System/exit 0)))

(defn mein
  "Externally available wrapper for -main."
  [args]
  (apply -main args)) ; have to use apply since already in a seq

(defn -stop
  "According to the manual, this function stop() will be called at 
  the end of the doLoop() method."
  [^Sim this]
  (let [^SimData sim-data$ (.simData this)]
    ;; You could do something here.
    ))

(defn -finish
  "Called by the SimState superclass automatically when it decides to end the
  steps (e.g. because you used the -for parameter on the command line, or you
  stop the run in the GUI).  Note that this function should not call the 
  corresponding function in the superclass; that function will call this one.
  So if you want to call this function explicitly, you may want to do so by
  calling superFinish, which can be defined in the defsim statement above using
  :exposes-methods.  However, if you always use MASON capabilities to end 
  simulations (e.g.  using -for or -until on the command line), you don't need
  to call superFinish, and this will automatically get called (from line 662 of 
  MASON 19's SimState.java?)"
  [^Sim this]
  (let [^SimData sim-data$ (.simData this)
        ^Stoppable stoppable (:stoppable @sim-data$)]
    (.stop stoppable))) ; Note that this is not the stop() defined above; Stoppable is not a subclass of SimState.

;; Note finish is never called here.  Stopping a simulation in any
;; normal MASON way will result in finish() above being called.
(defn run-sim
  "Runs the simulation: After setting up whatever is needed, calls
  Schedule.scheduleRepeating on a Steppable object that will repeatedly
  call the simulation code for a single step.  Other things that you want
  scheduled can be done here as well."
  [^Sim sim-sim rng sim-data$ seed]
  (let [^Schedule schedule (.schedule sim-sim)
        ;; Do additional setup here, check commandline parameters, etc.
        ;; This runs the simulation:
        ^Stoppable stoppable (.scheduleRepeating schedule Schedule/EPOCH 0 ; epoch = starting at beginning, 0 means run this first during timestep
                                      (reify Steppable 
                                        (step [this sim-state]
                                          (swap! sim-data$ assoc :curr-step (curr-step sim-sim)) ; make current tick available to popenv
                                          (swap! sim-data$ update :popenv pe/next-popenv rng sim-data$))))]
    (swap! sim-data$ assoc :stoppable stoppable))) ; store this to make available to finish()

(defn -start
  "Function that's called to (re)start a new simulation run."
  [^Sim this]
  (.superStart this)
  ;; Construct core data structures of the simulation:
  (let [^SimData sim-data$ (.simData this)
        ^MersenneTwisterFast rng (.-random this)
        seed (.seed this)]
    ;; If user passed commandline options, use them to set parameters, rather than defaults:
    (when (and @commandline$ (not (:in-gui @sim-data$))) ; see issue #56 in github for the logic here
      (set-sim-data-from-commandline! this commandline$))
    (swap! sim-data$ assoc :seed seed)
    (pe/setup-popenv-config! sim-data$)
    (swap! sim-data$ assoc :popenv (pe/make-popenv rng sim-data$)) ; create new popenv
    ;; Run it:
    (run-sim this rng sim-data$ seed)))
