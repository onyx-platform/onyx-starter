(ns onyx-starter.workflows.sample-workflow)

;;; The workflow of an Onyx job describes the graph of all possible
;;; tasks that data can flow between.

(def workflow
  [[:in :split-by-spaces]
   [:split-by-spaces :mixed-case]
   [:mixed-case :loud]
   [:mixed-case :question]
   [:loud :loud-output]
   [:question :question-output]])
