create table offsets
(
  current_offset BIGINT default 0
);

insert into offsets
values (
        0
       );