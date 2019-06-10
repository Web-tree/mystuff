import {Component, OnInit} from '@angular/core';
import {StuffService} from "../_services/stuff.service";

import{ Stuff } from '../_models/Stuff';

@Component({
  selector: 'stuff-list',
  templateUrl: './stuff-list.component.html',
  styleUrls: ['./stuff-list.component.css']
})
export class StuffListComponent implements OnInit {

  public stuffList: Stuff[] = [];

  constructor(private stuffService: StuffService) {
  }

  ngOnInit() {
    this.stuffService.getMyStuff().subscribe(stuffList => {
      this.stuffList = stuffList;
    });
  }

  delete(id: number) {
    console.log(id);
    this.stuffService.delete(id).subscribe(() => this._deleteFromCache(id));
  }

  private _deleteFromCache(id) {
    console.log(this.stuffList);
    for (let stuffIndex in this.stuffList) {
      let stuff = this.stuffList[stuffIndex];
      console.log(stuff);
      if (stuff['id'] === id) {
        this.stuffList.splice(+stuffIndex, 1);
      }
    }
  }
}
