#pragma once

#include <iostream>

#include "Common.h"

namespace magpie
{
  // A simple queue using a linked-list of nodes.
  template <class T>
  class Queue
  {
  public:
    Queue()
    : head_(NULL),
      tail_(NULL) {}

    // Gets whether or not the queue is empty.
    bool isEmpty() const { return head_ == NULL; }

    // Adds the given item to the end of the queue.
    void enqueue(const T& item)
    {
      QueueNode* node = new QueueNode(item);

      if (head_ == NULL)
      {
        // No items in the queue yet.
        head_ = node;
        tail_ = node;
      }
      else
      {
        tail_->next = node;
        tail_ = node;
      }
    }

    // Removes the first item from the queue.
    T dequeue()
    {
      ASSERT(head_ != NULL, "Cannot read from an empty queue.");

      QueueNode* node = head_;
      const T& item = node->item;

      head_ = head_->next;
      if (head_ == NULL) tail_ = NULL;

      delete node;

      return item;
    }

    // Indicates that the given array of objects is reachable and should be
    // preserved during garbage collection. T should be a gc type.
    void reach()
    {
      QueueNode* node = head_;
      while (node != NULL)
      {
        node->item.reach();
        node = node->next;
      }
    }
    
  private:
    struct QueueNode
    {
      QueueNode(const T& item)
      : item(item),
        next(NULL)
      {}
      
      T item;
      QueueNode* next;
    };

    QueueNode* head_;
    QueueNode* tail_;

    NO_COPY(Queue);
  };
}

